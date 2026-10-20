const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const CHROME = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";

async function capture({ url, width = 1920, height = 1080, output, waitMs = 3500, evalJs = null }) {
    const port = 9222 + Math.floor(Math.random() * 500);
    const tempDir = path.join(__dirname, 'chrome-temp-' + port);
    const chromeProc = spawn(CHROME, [
        '--headless=new',
        '--disable-gpu',
        `--remote-debugging-port=${port}`,
        `--user-data-dir=${tempDir}`,
        '--no-first-run',
        '--no-default-browser-check'
    ]);

    try {
        let wsUrl = null;
        for (let i = 0; i < 30; i++) {
            await new Promise(r => setTimeout(r, 200));
            try {
                const res = await fetch(`http://127.0.0.1:${port}/json/version`);
                if (res.ok) {
                    const data = await res.json();
                    wsUrl = data.webSocketDebuggerUrl;
                    break;
                }
            } catch (e) {}
        }

        if (!wsUrl) throw new Error('Chrome did not start debugging port');

        const targetRes = await fetch(`http://127.0.0.1:${port}/json/new?${encodeURIComponent(url)}`, { method: 'PUT' });
        const targetData = await targetRes.json();
        const tabWsUrl = targetData.webSocketDebuggerUrl;

        const ws = new WebSocket(tabWsUrl);
        await new Promise((resolve, reject) => {
            ws.onopen = resolve;
            ws.onerror = reject;
        });

        let id = 1;
        const send = (method, params = {}) => new Promise((resolve) => {
            const msgId = id++;
            const handler = (evt) => {
                const msg = JSON.parse(evt.data);
                if (msg.id === msgId) {
                    ws.removeEventListener('message', handler);
                    resolve(msg.result);
                }
            };
            ws.addEventListener('message', handler);
            ws.send(JSON.stringify({ id: msgId, method, params }));
        });

        await send('Page.enable');
        await send('Emulation.setDeviceMetricsOverride', {
            width,
            height,
            deviceScaleFactor: 1,
            mobile: width < 600
        });

        // Wait until authenticated and appShell is visible
        let authed = false;
        for (let i = 0; i < 30; i++) {
            await new Promise(r => setTimeout(r, 400));
            const check = await send('Runtime.evaluate', {
                expression: "Boolean(document.getElementById('appShell') && document.getElementById('appShell').style.display === 'flex')"
            });
            if (check && check.result && check.result.value === true) {
                authed = true;
                break;
            }
        }
        console.log('Authentication state:', authed ? 'SUCCESS' : 'TIMEOUT');
        await new Promise(r => setTimeout(r, 1500));

        if (evalJs) {
            await send('Runtime.evaluate', { expression: evalJs, awaitPromise: true });
            await new Promise(r => setTimeout(r, 800));
        }

        const shot = await send('Page.captureScreenshot', { format: 'png' });
        fs.writeFileSync(output, Buffer.from(shot.data, 'base64'));
        console.log(`Saved screenshot to ${output} (${fs.statSync(output).size} bytes)`);

        ws.close();
    } finally {
        chromeProc.kill();
        try {
            fs.rmSync(tempDir, { recursive: true, force: true });
        } catch (e) {}
    }
}

async function main() {
    const action = process.argv[2] || 'overview';
    const cwd = process.cwd();

    if (action === 'overview') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=overview',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_overview_1920.png'),
            waitMs: 4000
        });
    } else if (action === 'rooms_grid') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=rooms',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_rooms_grid_1920.png'),
            waitMs: 4000
        });
    } else if (action === 'rooms_table') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=rooms',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_rooms_table_1920.png'),
            waitMs: 4000,
            evalJs: "toggleRoomViewMode()"
        });
    } else if (action === 'services') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=services',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_services_1920.png'),
            waitMs: 4000
        });
    } else if (action === 'events') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=events',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_events_1920.png'),
            waitMs: 4000
        });
    } else if (action === 'operations') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=operations',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_operations_1920.png'),
            waitMs: 4000
        });
    } else if (action === 'connection') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=connection',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_connection_1920.png'),
            waitMs: 4000
        });
    } else if (action === 'system') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=system',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_system_1920.png'),
            waitMs: 4000
        });
    } else if (action === 'responsive_1440') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=overview',
            width: 1440,
            height: 900,
            output: path.join(cwd, 'shot_overview_1440.png'),
            waitMs: 4000
        });
    } else if (action === 'responsive_1366') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=overview',
            width: 1366,
            height: 768,
            output: path.join(cwd, 'shot_overview_1366.png'),
            waitMs: 4000
        });
    } else if (action === 'responsive_mobile') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=overview',
            width: 375,
            height: 812,
            output: path.join(cwd, 'shot_overview_mobile.png'),
            waitMs: 4000
        });
    } else if (action === 'room_drawer') {
        await capture({
            url: 'http://localhost:8085/?autologin=true&tab=rooms',
            width: 1920,
            height: 1080,
            output: path.join(cwd, 'shot_room_drawer.png'),
            waitMs: 4000,
            evalJs: "openRoomDrawer('101')"
        });
    } else if (action === 'all') {
        const tasks = ['overview', 'rooms_grid', 'rooms_table', 'services', 'events', 'operations', 'connection', 'system', 'room_drawer', 'responsive_1440', 'responsive_1366', 'responsive_mobile'];
        for (const t of tasks) {
            console.log(`--- Running ${t} ---`);
            const sub = spawn('node', [__filename, t], { stdio: 'inherit' });
            await new Promise(r => sub.on('close', r));
        }
    }
}

main().catch(console.error);
