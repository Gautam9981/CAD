// API Documentation Board JavaScript
// Loads and displays API data with search and filtering capabilities

let apiData = {};
let filteredData = {};

// Main initialization
document.addEventListener('DOMContentLoaded', () => {
    loadAPIData();

    initializeEventListeners();
});

// Load API data from JSON file
async function loadAPIData() {
    try {
        const response = await fetch('api-data.json');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        apiData = await response.json();
        filteredData = { ...apiData };

        populatePackageFilter();
        renderNavigation();
        renderContent(); // Render all classes and methods on initial load
        updateStats();

        // Check for hash navigation
        if (window.location.hash) {
            const className = window.location.hash.substring(1);
            scrollToClass(className);
        }
    } catch (error) {
        console.error('Error loading API data:', error);
        showError(`Failed to load API documentation: ${error.message}`);
    }
}

// Initialize event listeners
function initializeEventListeners() {
    const searchInput = document.getElementById('searchInput');
    const packageFilter = document.getElementById('packageFilter');
    const visibilityFilter = document.getElementById('visibilityFilter');
    const clearFilters = document.getElementById('clearFilters');

    // Search with debounce
    let searchTimeout;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => applyFilters(), 300);
    });

    packageFilter.addEventListener('change', applyFilters);
    visibilityFilter.addEventListener('change', applyFilters);

    clearFilters.addEventListener('click', () => {
        searchInput.value = '';
        packageFilter.value = '';
        visibilityFilter.value = '';
        applyFilters();
    });

    // Hash navigation
    window.addEventListener('hashchange', () => {
        const className = window.location.hash.substring(1);
        if (className) {
            scrollToClass(className);
        }
    });
}

// Populate package filter dropdown
function populatePackageFilter() {
    const packages = new Set();
    Object.values(apiData).forEach(classInfo => {
        packages.add(classInfo.package);
    });

    const packageFilter = document.getElementById('packageFilter');
    Array.from(packages).sort().forEach(pkg => {
        const option = document.createElement('option');
        option.value = pkg;
        option.textContent = pkg;
        packageFilter.appendChild(option);
    });
}

// Apply search and filters
function applyFilters() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const packageFilter = document.getElementById('packageFilter').value;
    const visibilityFilter = document.getElementById('visibilityFilter').value;

    filteredData = {};

    Object.entries(apiData).forEach(([fullClassName, classInfo]) => {
        // Package filter
        if (packageFilter && classInfo.package !== packageFilter) {
            return;
        }

        // Search filter (search in class name, method names, parameters, return types)
        if (searchTerm) {
            const searchableText = [
                classInfo.className.toLowerCase(),
                classInfo.package.toLowerCase(),
                ...classInfo.methods.map(m =>
                    `${m.name} ${m.returnType} ${m.parameters.map(p => p.type + ' ' + p.name).join(' ')}`
                ).join(' ').toLowerCase()
            ].join(' ');

            if (!searchableText.includes(searchTerm)) {
                return;
            }
        }

        // Visibility filter for methods
        let filteredMethods = classInfo.methods;
        if (visibilityFilter) {
            filteredMethods = classInfo.methods.filter(m => m.visibility === visibilityFilter);
        }

        // Only include class if it has methods after filtering
        if (filteredMethods.length > 0 || classInfo.constructors.length > 0) {
            filteredData[fullClassName] = {
                ...classInfo,
                methods: filteredMethods
            };
        }
    });

    renderNavigation();
    renderContent();
    updateStats();
}

// Render navigation sidebar
function renderNavigation() {
    const classNav = document.getElementById('classNav');
    classNav.innerHTML = '';

    // Group by package
    const packageGroups = {};
    Object.entries(filteredData).forEach(([fullClassName, classInfo]) => {
        if (!packageGroups[classInfo.package]) {
            packageGroups[classInfo.package] = [];
        }
        packageGroups[classInfo.package].push({ fullClassName, classInfo });
    });

    // Render each package group
    Object.entries(packageGroups).sort().forEach(([packageName, classes]) => {
        const packageGroup = document.createElement('div');
        packageGroup.className = 'package-group';

        const packageHeader = document.createElement('div');
        packageHeader.className = 'package-header';
        packageHeader.textContent = packageName;
        packageGroup.appendChild(packageHeader);

        classes.sort((a, b) => a.classInfo.className.localeCompare(b.classInfo.className))
            .forEach(({ fullClassName, classInfo }) => {
                const link = document.createElement('a');
                link.href = `#${fullClassName}`;
                link.className = 'class-link';
                link.textContent = classInfo.className;
                link.addEventListener('click', (e) => {
                    e.preventDefault();
                    scrollToClass(fullClassName);
                    window.location.hash = fullClassName;
                });
                packageGroup.appendChild(link);
            });

        classNav.appendChild(packageGroup);
    });

    document.getElementById('visibleClassCount').textContent = Object.keys(filteredData).length;
}

// Render content area
function renderContent() {
    const apiContent = document.getElementById('apiContent');

    if (Object.keys(filteredData).length === 0) {
        apiContent.innerHTML = `
            <div class="welcome-message">
                <div class="welcome-icon">🔍</div>
                <h2>No results found</h2>
                <p>Try adjusting your search or filters</p>
            </div>
        `;
        return;
    }

    apiContent.innerHTML = '';

    Object.entries(filteredData).sort((a, b) =>
        a[1].className.localeCompare(b[1].className)
    ).forEach(([fullClassName, classInfo]) => {
        const classSection = createClassSection(fullClassName, classInfo);
        apiContent.appendChild(classSection);
    });
}

// Create a class section
function createClassSection(fullClassName, classInfo) {
    const section = document.createElement('section');
    section.className = 'class-section';
    section.id = fullClassName;

    // Class header
    const header = document.createElement('div');
    header.className = 'class-header';
    header.innerHTML = `
        <h2 class="class-title">${classInfo.className}</h2>
        <div class="class-package">${classInfo.package}</div>
    `;
    section.appendChild(header);

    // Constructors
    if (classInfo.constructors.length > 0) {
        // Filter: Keep only constructors with the maximum number of parameters
        const maxParams = Math.max(...classInfo.constructors.map(c => c.parameters ? c.parameters.length : 0));
        const filteredConstructors = classInfo.constructors.filter(c => (c.parameters ? c.parameters.length : 0) === maxParams);

        const constructorsSection = document.createElement('div');
        constructorsSection.className = 'methods-section';
        constructorsSection.innerHTML = `
            <h3 class="section-title">
                Constructors
                <span class="method-count">(${filteredConstructors.length})</span>
            </h3>
        `;

        const constructorsGrid = document.createElement('div');
        constructorsGrid.className = 'methods-grid';
        filteredConstructors.forEach(constructor => {
            constructorsGrid.appendChild(createMethodCard(constructor, true));
        });
        constructorsSection.appendChild(constructorsGrid);
        section.appendChild(constructorsSection);
    }

    // Methods
    if (classInfo.methods.length > 0) {
        const methodsSection = document.createElement('div');
        methodsSection.className = 'methods-section';
        methodsSection.innerHTML = `
            <h3 class="section-title">
                Methods
                <span class="method-count">(${classInfo.methods.length})</span>
            </h3>
        `;

        const methodsGrid = document.createElement('div');
        methodsGrid.className = 'methods-grid';
        classInfo.methods.forEach(method => {
            methodsGrid.appendChild(createMethodCard(method, false));
        });
        methodsSection.appendChild(methodsGrid);
        section.appendChild(methodsSection);
    }

    return section;
}

// Create a method card
function createMethodCard(method, isConstructor) {
    const card = document.createElement('div');
    card.className = 'method-card';

    const visibilityClass = `visibility-${method.visibility.toLowerCase().replace('-', '-')}`;

    card.innerHTML = `
        <div class="method-header">
            <span class="visibility-badge ${visibilityClass}">${method.visibility}</span>
            <div class="method-name">${method.name}</div>
            ${method.static ? '<span class="static-badge">STATIC</span>' : ''}
        </div>
        
        <div class="method-description">
            ${escapeHtml(getDescription(method, isConstructor))}
        </div>
        
        <div class="method-details">
            ${!isConstructor ? `
                <div class="detail-row">
                    <div class="detail-label">Return Type:</div>
                    <div class="detail-value return-type">${escapeHtml(method.returnType)}</div>
                </div>
            ` : ''}
            
            <div class="detail-row">
                <div class="detail-label">Parameters:</div>
                <div class="detail-value">
                    ${method.parameters.length > 0 ? `
                        <div class="parameters-list">
                            ${method.parameters.map(p => `
                                <div class="parameter-item">
                                    <div class="param-signature">
                                        <span class="param-type">${escapeHtml(p.type)}</span>
                                        <span class="param-name">${escapeHtml(p.name)}</span>
                                    </div>
                                    <div class="param-description">${escapeHtml(getParamDescription(p))}</div>
                                </div>
                            `).join('')}
                        </div>
                    ` : '<span class="no-params">None</span>'}
                </div>
            </div>
        </div>
        
        <div class="method-signature">
            <code>${highlightSignature(method.signature)}</code>
        </div>
    `;

    return card;
}

// Get description (use inferred if existing is poor)
function getDescription(method, isConstructor) {
    let desc = method.description;
    const inferred = inferDescription(method, isConstructor);

    // Decision logic: Use inferred if no existing or if existing is trivial
    if (!desc) {
        desc = inferred;
    } else {
        const cleanExisting = desc.toLowerCase().replace(/[^a-z]/g, '');
        const cleanName = method.name.toLowerCase().replace(/[^a-z]/g, '');

        // Extract verb/noun for trivial check
        const words = method.name.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase().split(' ');
        const verb = words[0];
        const noun = words.slice(1).join(' ');
        const name = method.name;

        if (cleanExisting === cleanName ||
            cleanExisting === cleanName + 'command' ||
            (desc.length < 15 && !desc.includes(' ')) ||
            (desc.length < 25 && desc.match(/^(get|set|gets|sets|is|has)\s+(the\s+)?\w+\.?$/i)) ||
            desc.toLowerCase().startsWith("handle " + method.name.replace('handle', '').toLowerCase()) ||
            desc.toLowerCase().startsWith("notify " + method.name.replace('notify', '').toLowerCase()) ||
            desc.toLowerCase().startsWith("represents") ||
            desc.toLowerCase().trim() === "standard " + method.name.toLowerCase() ||
            desc.toLowerCase().includes("instance of") ||
            // New trivial overrides
            desc.toLowerCase().startsWith(verb + " " + noun.toLowerCase()) || // e.g., "Init component"
            (words.length === 1 && desc.toLowerCase().replace('.', '') === name.toLowerCase()) // e.g. "reset" -> "Reset."
        ) {
            desc = inferred;
        }
    }

    // Common cleanup: Strip trailing period and whitespace
    if (desc) {
        return desc.replace(/\.+\s*$/, '');
    }

    return desc;
}

// Get parameter description
function getParamDescription(param) {
    if (param.description && param.description.trim().length > 0) {
        return param.description;
    }
    return inferParamDescription(param);
}

// Infer parameter description
function inferParamDescription(param) {
    const name = param.name.toLowerCase();
    const type = param.type.toLowerCase();

    // Sketch and drawing objects
    if (name === 'sketch') return 'Target sketch containing entities and constraints';
    if (name === 'entity') return 'Sketch entity (line, circle, point, etc.) to operate on';
    if (name === 'entitytype') return 'Type of entity to create (e.g., LINE, CIRCLE, POINT)';
    if (name === 'constraint') return 'Geometric constraint to apply or modify';
    if (name === 'dimension') return 'Measurement dimension to add or update';

    // Common abbreviations
    if (name === 'x') return 'X coordinate in the sketch plane';
    if (name === 'y') return 'Y coordinate in the sketch plane';
    if (name === 'z') return 'Z coordinate (depth/extrusion distance)';
    if (name === 'w' || name === 'width') return 'Width value in current units';
    if (name === 'h' || name === 'height') return 'Height value in current units';
    if (name === 'l' || name === 'length') return 'Length value in current units';
    if (name === 'radius') return 'Radius for circular or arc entities';
    if (name === 'angle') return 'Angle in degrees';

    // Points
    if (name === 'p' || name === 'p1' || name === 'start') return 'Starting point coordinates';
    if (name === 'p2' || name === 'end') return 'Ending point coordinates';
    if (name === 'pt' || name === 'point') return 'Point entity or coordinates';
    if (name === 'center') return 'Center point of the shape';

    // Graphics/Events
    if (name === 'gl' || name === 'g' || name === 'graphics' || name === 'gc') return 'OpenGL graphics context for rendering';
    if (name === 'e' || name === 'event' || name === 'evt') return 'User input event (mouse, keyboard)';

    // Materials and properties
    if (name === 'material') return 'Material definition with properties (density, strength, etc.)';
    if (name === 'color') return 'RGB color value for rendering';
    if (name === 'density') return 'Material density in kg/m³';
    if (name === 'name') return 'Descriptive name or identifier';

    // Common types
    if (name === 'text' || name === 'msg' || name === 'message') return 'Text content or message string';
    if (name === 'label') return 'Display label text';
    if (name === 'value') return 'Numeric value or measurement';
    if (name === 'idx' || name === 'index') return 'Zero-based array index';
    if (name === 'id') return 'Unique identifier';
    if (name === 'visible') return 'Visibility flag (true = shown, false = hidden)';
    if (name === 'selected') return 'Selection state flag';

    // Default fallback - be more descriptive
    const readable = param.name.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase();
    return `${readable.charAt(0).toUpperCase() + readable.slice(1)} value`;
}

// Infer description from method name
function inferDescription(method, isConstructor) {
    if (isConstructor) {
        return `Creates a new instance of ${method.name}`;
    }

    const name = method.name;

    // Split camelCase
    const words = name.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase().split(' ');

    // Helper to capitalize first letter
    const capitalize = (s) => s.charAt(0).toUpperCase() + s.slice(1);

    const verb = words[0];
    const noun = words.slice(1).join(' ');

    // Improve noun for single letter variables or common abbreviations
    let enhancedNoun = noun || 'value';

    // Exact noun matches
    if (noun === 'x') enhancedNoun = 'X coordinate';
    else if (noun === 'y') enhancedNoun = 'Y coordinate';
    else if (noun === 'z') enhancedNoun = 'Z coordinate';
    else if (noun === 'w' || noun === 'width') enhancedNoun = 'width value';
    else if (noun === 'h' || noun === 'height') enhancedNoun = 'height value';
    else if (noun === 'l' || noun === 'length') enhancedNoun = 'length value';
    else if (noun === 'id') enhancedNoun = 'unique identifier';
    else if (noun === 'name') enhancedNoun = 'name identifier';
    else if (noun === 'start' || noun === 'p1') enhancedNoun = 'start point';
    else if (noun === 'end' || noun === 'p2') enhancedNoun = 'end point';
    else if (noun === 'p' || noun === 'pt' || noun === 'point') enhancedNoun = 'point object';
    else if (noun === 'type') enhancedNoun = 'type identifier';
    else if (noun === 'count') enhancedNoun = 'total count';
    else if (noun === 'msg' || noun === 'message') enhancedNoun = 'message text';

    // Handle specific verbs
    if (verb === 'get') {
        return `Retrieves the ${enhancedNoun}`;
    }
    if (verb === 'set') {
        return `Sets the ${enhancedNoun}`;
    }
    if (verb === 'is' || verb === 'has' || verb === 'can') {
        return `Checks if ${noun ? 'it ' + name.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase() : 'condition is true'}`;
    }
    if (verb === 'add') {
        return `Adds a new ${noun || 'item'} to the collection`;
    }
    if (verb === 'remove' || verb === 'delete') {
        return `Removes the specified ${noun || 'item'}`;
    }
    if (verb === 'create' || verb === 'make' || verb === 'build') {
        return `Constructs and returns a new ${noun || 'object'}`;
    }
    if (verb === 'compute' || verb === 'calculate') {
        return `Calculates the ${noun || 'value'} based on current state`;
    }
    if (verb === 'solve') {
        return `Solves the active limits or equations`;
    }
    if (verb === 'update') {
        return `Updates the ${noun || 'internal state'}`;
    }
    if (verb === 'handle') {
        return `Executes the logic for the '${noun}' command or event`;
    }
    if (verb === 'render' || verb === 'draw') {
        return `Renders the ${noun || 'component'} to the graphics context`;
    }
    if (verb === 'export') {
        return `Exports the ${noun || 'data'} to an external file format`;
    }
    if (verb === 'load' || verb === 'import') {
        return `Loads ${noun || 'data'} from an external source`;
    }
    if (verb === 'parse') {
        return `Parses the ${noun || 'input'} string`;
    }
    if (verb === 'convert') {
        return `Converts the object to ${noun || 'another format'}`;
    }
    if (verb === 'edit' || verb === 'modify' || verb === 'change') {
        return `Modifies the properties or configuration of the ${noun || 'object'}`;
    }
    if (verb === 'init' || verb === 'initialize') {
        return `Initializes the ${noun || 'component'} and prepares it for use`;
    }
    if (verb === 'reset' || verb === 'clear') {
        return `Resets the ${noun || 'state'} to its default values`;
    }
    if (verb === 'check' || verb === 'validate' || verb === 'verify') {
        return `Validates the ${noun || 'condition'} and returns the result`;
    }
    if (verb === 'refresh' || verb === 'reload') {
        return `Refreshes the ${noun || 'view'} to reflect current state`;
    }
    if (verb === 'select' || verb === 'deselect') {
        return `Updates the selection state of the ${noun || 'item'}`;
    }
    if (verb === 'toggle') {
        return `Toggles the state of the ${noun || 'option'}`;
    }
    if (verb === 'apply') {
        return `Applies the ${noun || 'configuration'} changes`;
    }
    if (verb === 'process') {
        return `Processes the provided ${noun || 'data'}`;
    }

    // exact method name checks
    if (name === 'execute') return "Executes the command operation";
    if (name === 'undo') return "Reverses the effects of this command";
    if (name === 'redo') return "Re-applies the effects of this command";
    if (name === 'toString') return "Returns a string representation of this object";
    if (name === 'hashCode') return "Returns a hash code value for the object";
    if (name === 'equals') return "Indicates whether some other object is equal to this one";
    if (name === 'run') return "Executes the runnable task";

    // Default: "Do Something" -> "Do something"
    // If it's a single word and not a common verb, try to deduce meaning
    if (words.length === 1) {
        // If void return and no params, it might be an action like "init" or "reset"
        if (method.returnType === 'void') {
            return `Performs the ${name} operation`;
        }
        // If it returns something, it might be a getter-like: e.g. "length()"
        if (method.returnType !== 'void' && method.parameters.length === 0) {
            return `Gets the ${name} of the object`;
        }
    }

    return capitalize(words.join(' '));
}


// Highlight syntax in signature
function highlightSignature(signature) {
    let highlighted = escapeHtml(signature);

    // Highlight visibility modifiers
    highlighted = highlighted.replace(/\b(public|private|protected|package-private)\b/g,
        '<span class="visibility-badge">$1</span>');

    // Highlight static
    highlighted = highlighted.replace(/\bstatic\b/g,
        '<span class="static-badge">static</span>');

    return highlighted;
}

// Scroll to a specific class
function scrollToClass(fullClassName) {
    const element = document.getElementById(fullClassName);
    if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });

        // Update active state in sidebar
        document.querySelectorAll('.class-link').forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${fullClassName}`) {
                link.classList.add('active');
            }
        });
    }
}

// Update statistics
function updateStats() {
    const totalClasses = Object.keys(filteredData).length;
    const totalMethods = Object.values(filteredData).reduce(
        (sum, classInfo) => sum + classInfo.methods.length + classInfo.constructors.length,
        0
    );

    document.getElementById('classCount').textContent = `${totalClasses} Class${totalClasses !== 1 ? 'es' : ''}`;
    document.getElementById('methodCount').textContent = `${totalMethods} Method${totalMethods !== 1 ? 's' : ''}`;
}

// Show error message
function showError(message) {
    const apiContent = document.getElementById('apiContent');
    apiContent.innerHTML = `
        <div class="welcome-message">
            <div class="welcome-icon" style="color: var(--accent-error);">⚠️</div>
            <h2 style="color: var(--accent-error);">Error</h2>
            <p>${message}</p>
        </div>
    `;
}

// Utility: Escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
