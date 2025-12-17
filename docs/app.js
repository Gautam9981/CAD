// App logic for CAD Codex
let codex = {};

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await fetch('codex.json');
        codex = await response.json();

        const path = window.location.pathname;
        if (path.endsWith('flow.html')) {
            renderFlow();
        } else {
            renderAPI();
            initSearch();
        }

        updateStats();
    } catch (e) {
        console.error("Failed to load Codex:", e);
    }
});

function renderAPI(filter = "") {
    const grid = document.getElementById('apiGrid');
    if (!grid) return;

    grid.innerHTML = '';

    const sortedClasses = Object.values(codex).sort((a, b) => a.name.localeCompare(b.name));

    sortedClasses.forEach(cls => {
        // Filter logic
        const matches = cls.name.toLowerCase().includes(filter) ||
            cls.description.toLowerCase().includes(filter) ||
            cls.methods.some(m => m.name.toLowerCase().includes(filter));

        if (!matches && filter) return;

        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `
            <div class="card-header">
                <div>
                    <div class="class-title">${cls.name}</div>
                    <div class="class-package">${cls.package}</div>
                </div>
            </div>
            <div class="card-desc">${cls.description}</div>
            
            <table class="methods-table">
                ${cls.methods.slice(0, 5).map(m => `
                    <tr class="method-row">
                        <td class="method-cell" style="width: 20px;">
                            <span class="visibility vis-${m.visibility === 'package-private' ? 'package' : m.visibility}"></span>
                        </td>
                        <td class="method-cell">
                            <div class="method-sig">${m.name}<span style="color:var(--text-muted)">(${m.params})</span></div>
                            <div class="method-desc">${m.description}</div>
                            ${m.connections.length > 0 ?
                `<div style="margin-top:4px;">${m.connections.map(c =>
                    `<span class="connection-badge">→ ${c}</span>`
                ).join('')}</div>`
                : ''}
                        </td>
                    </tr>
                `).join('')}
                ${cls.methods.length > 5 ? `
                    <tr><td colspan="2" style="padding-top:10px; text-align:center; color:var(--text-muted); font-size:0.8rem;">
                        + ${cls.methods.length - 5} more methods
                    </td></tr>
                ` : ''}
            </table>
        `;
        grid.appendChild(card);
    });
}

function initSearch() {
    const search = document.getElementById('search');
    if (!search) return;

    search.addEventListener('input', (e) => {
        renderAPI(e.target.value.toLowerCase());
    });
}

function updateStats() {
    const classes = Object.keys(codex).length;
    const methods = Object.values(codex).reduce((acc, c) => acc + c.methods.length, 0);

    const statClasses = document.getElementById('statClasses');
    if (statClasses) statClasses.textContent = `${classes} Classes`;

    const statMethods = document.getElementById('statMethods');
    if (statMethods) statMethods.textContent = `${methods} Methods`;
}

async function renderFlow() {
    const chart = document.getElementById('flowChart');
    if (!chart) return;

    // Generate Mermaid Graph
    let graph = "graph TD\n";
    graph += "    node [shape=rect, style=filled, color=#161b22, fillcolor=#161b22, fontcolor=#c9d1d9, fontname='Inter']\n";
    graph += "    edge [color=#30363d]\n\n";

    Object.values(codex).forEach(cls => {
        // Add Node
        graph += `    ${cls.name}["${cls.name}"]\n`;

        // Add Edges (Aggregate dependencies)
        const deps = new Set(cls.dependencies);
        cls.methods.forEach(m => m.connections.forEach(c => deps.add(c)));

        deps.forEach(dep => {
            if (codex[Object.keys(codex).find(k => k.endsWith('.' + dep)) || dep]) { // Only link to known classes
                graph += `    ${cls.name} --> ${dep}\n`;
            }
        });
    });

    // Render
    chart.textContent = graph;
    if (window.mermaid) {
        chart.innerHTML = `<pre class="mermaid">${graph}</pre>`;
        await window.mermaid.run();
    }
}
