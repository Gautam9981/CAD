// App logic for CAD Flow Diagram
let codex = {};

document.addEventListener('DOMContentLoaded', async () => {
    // Check for embed mode
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('embed') === 'true') {
        document.body.classList.add('embedded');
    }

    try {
        const response = await fetch('codex.json');
        codex = await response.json();

        renderFlow();

        const statClasses = document.getElementById('statClasses');
        if (statClasses) statClasses.textContent = `${Object.keys(codex).length} Classes`;

    } catch (e) {
        console.error("Failed to load Codex:", e);
    }
});

function sanitizeId(str) {
    // Replace ALL non-alphanumeric chars with underscore to ensure valid Mermaid IDs
    // Also handle empty strings
    if (!str) return 'unknown';
    return str.replace(/[^a-zA-Z0-9]/g, '_');
}

async function renderFlow() {
    const chart = document.getElementById('flowChart');
    if (!chart) return;

    // Generate Mermaid Graph
    // Use flowchart to support autoscaling better than 'graph'
    let graph = "flowchart TD\n";
    graph += "    node [shape=rect, style=filled, color=#161b22, fillcolor=#161b22, fontcolor=#c9d1d9, fontname='Inter']\n";
    graph += "    edge [color=#30363d]\n\n";

    // Track added nodes to avoid duplicates if something weird happens
    const addedNodes = new Set();

    const classes = Object.values(codex);

    // 1. Add all nodes first
    classes.forEach(cls => {
        const id = sanitizeId(cls.name);
        if (!addedNodes.has(id)) {
            // Escape the label
            const label = cls.name.replace(/"/g, "'");
            graph += `    ${id}["${label}"]\n`;
            addedNodes.add(id);
        }
    });

    // 2. Add edges
    classes.forEach(cls => {
        const sourceId = sanitizeId(cls.name);
        const deps = new Set(cls.dependencies || []);

        // Add specific method connections
        if (cls.methods) {
            cls.methods.forEach(m => {
                if (m.connections) {
                    m.connections.forEach(c => deps.add(c));
                }
            });
        }

        deps.forEach(depName => {
            // Validate dependency target exists
            const targetCls = classes.find(c => c.name === depName || c.name.endsWith('.' + depName));

            if (targetCls) {
                const targetId = sanitizeId(targetCls.name);
                // Avoid self-loops if desired, or keep them
                if (sourceId !== targetId) {
                    graph += `    ${sourceId} --> ${targetId}\n`;
                }
            }
        });
    });

    // Render
    chart.textContent = graph;

    if (window.mermaid) {
        try {
            // Clear and insert
            chart.innerHTML = `<pre class="mermaid" style="width:100%; height:100%;">${graph}</pre>`;
            await window.mermaid.run();

            // Adjust SVG to fit
            const svg = chart.querySelector('svg');
            if (svg) {
                svg.style.width = '100%';
                svg.style.height = '100%';
                svg.removeAttribute('max-width');
            }
        } catch (e) {
            console.error(e);
            chart.innerHTML = `<div style="color:#ff6b6b; padding:2rem;">
                                <h3>Visualization Error</h3>
                                <p>${e.message}</p>
                                <pre style="background:#111; padding:1rem; overflow:auto;">${graph}</pre>
                               </div>`;
        }
    }
}
