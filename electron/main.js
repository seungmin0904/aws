const { app, BrowserWindow, Tray, Menu, Notification, ipcMain, clipboard } = require('electron');
const path = require('path');
const fs = require('fs');
const AutoLaunch = require('auto-launch');
const os = require('os');

// GPU 캐시 충돌 방지
app.disableHardwareAcceleration();

// Electron 캐시 경로 지정
const customUserDataPath = path.join(os.tmpdir(), 'my-electron-app-cache');
if (!fs.existsSync(customUserDataPath)) fs.mkdirSync(customUserDataPath, { recursive: true });
app.setPath('userData', customUserDataPath);

let mainWindow;
let tray;
let settingsFile = path.join(app.getPath('userData'), 'settings.json');

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1000,
    height: 700,
    show: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
      webSecurity: false,
    },
  });

  // // ✅ React 개발 서버에 연결 (개발 중)
  // mainWindow.loadURL("http://localhost:5174").catch(err => {
  //   console.error("❌ React dev server 로드 실패:", err);
  // });

  // ✅ 프로덕션용 파일 로드
 mainWindow.loadFile(path.join(__dirname, '..', 'front', 'dist', 'index.html')).catch(err => {
  console.error("❌ index.html 로드 실패:", err);
});

  mainWindow.on('close', e => {
    e.preventDefault();
    mainWindow.hide();
  });

  mainWindow.on('minimize', e => {
    e.preventDefault();
    mainWindow.hide();
  });
}

app.whenReady().then(() => {
  createWindow();

  tray = new Tray(path.join(__dirname, 'icon.png'));
  tray.setToolTip('Electron App Running');
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: '열기', click: () => mainWindow.show() },
    { label: '종료', click: () => {
      tray.destroy();
      app.quit();
    }}
  ]));

  tray.on('double-click', () => mainWindow.show());

  new Notification({ title: '앱 실행됨', body: 'Electron 앱이 실행 중입니다.' }).show();

  const autoLauncher = new AutoLaunch({ name: 'MyElectronApp' });
  autoLauncher.enable().catch(console.error);
});

ipcMain.on('notify', (e, { title, body }) => {
  new Notification({ title, body }).show();
});

ipcMain.on('saveSetting', (e, { key, value }) => {
  let settings = {};
  if (fs.existsSync(settingsFile)) {
    try {
      settings = JSON.parse(fs.readFileSync(settingsFile));
    } catch {}
  }
  settings[key] = value;
  fs.writeFileSync(settingsFile, JSON.stringify(settings, null, 2));
});

ipcMain.handle('loadSetting', (e, key) => {
  if (fs.existsSync(settingsFile)) {
    try {
      const settings = JSON.parse(fs.readFileSync(settingsFile));
      return settings[key];
    } catch {}
  }
  return null;
});

app.on('window-all-closed', () => {});
