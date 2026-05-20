import { Routes, Route } from "react-router-dom";
import Layout from "../layout/Layout.tsx";
import Sidebar from "../layout/Sidebar.tsx";
import SideMenu from "./SideMenu.tsx";
import Content from "../layout/Content.tsx";
import ContentTopBar from "./ContentTopBar.tsx";
import AgentChatView from "./views/AgentChatView.tsx";
import KnowledgeBaseView from "./views/KnowledgeBaseView.tsx";
import MemoryView from "./views/MemoryView.tsx";

/** 主应用布局：左侧边栏 + 右侧内容区（顶栏含「我的记忆」） */
export default function AiteAgentLayout() {
  return (
    <Layout>
      <Sidebar>
        <SideMenu />
      </Sidebar>
      <Content>
        <div className="h-full flex flex-col min-h-0">
          <ContentTopBar />
          <div className="flex-1 min-h-0 overflow-hidden">
            <Routes>
              <Route path="/" element={<AgentChatView />} />
              <Route path="/agent" element={<AgentChatView />} />
              <Route path="/chat" element={<AgentChatView />} />
              <Route path="/chat/:chatSessionId" element={<AgentChatView />} />
              <Route path="/knowledge-base" element={<KnowledgeBaseView />} />
              <Route
                path="/knowledge-base/:knowledgeBaseId"
                element={<KnowledgeBaseView />}
              />
              <Route path="/memories" element={<MemoryView />} />
            </Routes>
          </div>
        </div>
      </Content>
    </Layout>
  );
}
