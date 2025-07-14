const { contextBridge, ipcRenderer, clipboard } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  isElectron: true,
  notify: (title, body) => ipcRenderer.send('notify', { title, body }),
  writeClipboard: text => clipboard.writeText(text),
  readClipboard: () => clipboard.readText(),
  saveSetting: (key, value) => ipcRenderer.send('saveSetting', { key, value }),
  loadSetting: key => ipcRenderer.invoke('loadSetting', key),
  getToken: () => localStorage.getItem("token"),  
  getUsername: () => localStorage.getItem("username"),  
  getName: () => localStorage.getItem("name"),  
  getRefreshToken: () => localStorage.getItem("refresh_token"),
  startRecording: () => ipcRenderer.send('startRecording'),
  stopRecording: () => ipcRenderer.send('stopRecording')
});
