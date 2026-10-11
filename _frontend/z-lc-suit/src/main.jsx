import React from 'react'
import ReactDOM from 'react-dom/client'
import {ConfigProvider} from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import 'antd/dist/reset.css'

// Router 只在 App.jsx 一处（main 不再包 BrowserRouter，双 Router 会炸渲染）
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <App/>
    </ConfigProvider>
  </React.StrictMode>,
)
