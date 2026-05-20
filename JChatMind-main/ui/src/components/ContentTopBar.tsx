import React from "react";
import { BulbOutlined } from "@ant-design/icons";
import { Button } from "antd";
import { useLocation, useNavigate } from "react-router-dom";

/**
 * 主内容区顶栏：「我的记忆」入口置于右侧，与左侧边栏职责分离
 */
const ContentTopBar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const isMemoriesActive = location.pathname.startsWith("/memories");

  return (
    <div className="h-12 shrink-0 border-b border-gray-200 flex items-center justify-end px-4 bg-white">
      <Button
        type={isMemoriesActive ? "primary" : "text"}
        icon={<BulbOutlined />}
        onClick={() => navigate("/memories")}
        className="select-none"
      >
        我的记忆
      </Button>
    </div>
  );
};

export default ContentTopBar;
