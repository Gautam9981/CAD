// App logic for CAD Flow Diagram
let codex = {};

document.addEventListener('DOMContentLoaded', async () => {
    // Check for embed mode
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('embed') === 'true') {
        document.body.classList.add('embedded');
    }

    try {
        const response = await fetch(`codex.json?v=${new Date().getTime()}`);
        codex = await response.json();

        renderDependencyCards();

        const statClasses = document.getElementById('statClasses');
        if (statClasses) statClasses.textContent = `${Object.keys(codex).length} Classes`;

    } catch (e) {
        console.error("Failed to load Codex:", e);
        const container = document.getElementById('dependencyList');
        if (container) container.innerHTML = "Failed to load system architecture.";
    }
});

function renderDependencyCards() {
    const container = document.getElementById('dependencyList');
    if (!container) return;

    let html = '';

    // Sort classes alphabetically
    const classes = Object.keys(codex).sort();

    classes.forEach(cls => {
        const data = codex[cls];

        // Collect all unique dependencies
        const dependencies = new Set();
        if (data.dependencies) {
            data.dependencies.forEach(d => dependencies.add(d));
        }
        if (data.methods) {
            data.methods.forEach(m => {
                if (m.connections) {
                    m.connections.forEach(c => dependencies.add(c));
                }
            });
        }
        // Filter out self-reference
        dependencies.delete(data.name);

        const depList = Array.from(dependencies).sort();

        // If no dependencies, maybe skip or show empty state? Let's show empty state.
        let depTableRows = '';

        if (depList.length === 0) {
            depTableRows = `<tr><td colspan="2" style="color:var(--text-secondary); font-style:italic; padding:12px;">No explicit dependencies detected.</td></tr>`;
        } else {
            depTableRows = depList.map(depName => {
                // Find "preview" description
                // Dependency might be a full class key or just a name.
                // Our extractor usually stores simple names in connections, but keys are full package names.
                // We try to find the class in codex.
                let depDescription = "No description available.";
                let fullDepKey = Object.keys(codex).find(k => codex[k].name === depName || k.endsWith('.' + depName));

                let isLinkable = false;

                if (fullDepKey && codex[fullDepKey]) {
                    // Use the class description
                    depDescription = codex[fullDepKey].description || "No description available.";
                    isLinkable = true;
                } else {
                    // It might be a Java std lib class or something we didn't parse fully
                    depDescription = "External or System Component";
                }

                // Truncate if too long (preview)
                if (depDescription.length > 80) {
                    depDescription = depDescription.substring(0, 77) + "...";
                }

                const nameHtml = isLinkable
                    ? `<a href="#" onclick="navigateToApi(event, '${depName}')" class="dep-link">${depName}</a>`
                    : `<span class="dep-static">${depName}</span>`;

                return `
                    <tr>
                        <td style="width: 30%; font-weight: 500">${nameHtml}</td>
                        <td style="width: 70%; color: var(--text-secondary); font-size: 0.9em">${depDescription}</td>
                    </tr>
                `;
            }).join('');
        }

        html += `
            <div class="class-card" style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); border-radius: 8px; overflow: hidden;">
                <div style="padding: 12px 16px; background: rgba(255,255,255,0.02); border-bottom: 1px solid rgba(255,255,255,0.05);">
                    <div style="font-weight: 600; font-size: 1.1em; color: var(--accent);">${data.name}</div>
                    <div style="font-size: 0.8em; color: var(--text-tertiary); font-family: monospace;">${data.package}</div>
                </div>
                <table class="dep-table" style="width:100%; border-collapse:collapse;">
                    ${depTableRows}
                </table>
            </div>
        `;
    });

    container.innerHTML = html;
}

// Global function for onclick handlers
window.navigateToApi = function (event, className) {
    event.preventDefault();
    // Post message to parent (index.html)
    window.parent.postMessage({ type: 'NAVIGATE_API', target: className }, '*');
};
