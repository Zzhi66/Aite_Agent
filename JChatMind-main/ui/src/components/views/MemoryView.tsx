import React, { useMemo, useState } from "react";
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
  Empty,
} from "antd";
import {
  BulbOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import { useLongTermMemories } from "../../hooks/useLongTermMemories.ts";
import { useAgents } from "../../hooks/useAgents.ts";
import type {
  LongTermMemoryType,
  LongTermMemoryVO,
} from "../../api/api.ts";

const { Title, Text, Paragraph } = Typography;

/** 记忆类型展示文案 */
const MEMORY_TYPE_LABEL: Record<LongTermMemoryType, string> = {
  PREFERENCE: "偏好",
  FACT: "事实",
};

const MEMORY_TYPE_OPTIONS = [
  { value: "PREFERENCE" as LongTermMemoryType, label: "偏好" },
  { value: "FACT" as LongTermMemoryType, label: "事实" },
];

interface MemoryFormValues {
  memoryType: LongTermMemoryType;
  content: string;
}

/**
 * 「我的记忆」页面：用户级长期记忆 CRUD，跨 Agent 展示
 */
const MemoryView: React.FC = () => {
  const [typeFilter, setTypeFilter] = useState<LongTermMemoryType | undefined>(
    undefined,
  );
  const {
    memories,
    loading,
    createMemoryHandle,
    updateMemoryHandle,
    deleteMemoryHandle,
  } = useLongTermMemories(typeFilter);
  const { agents } = useAgents();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingMemory, setEditingMemory] = useState<LongTermMemoryVO | null>(
    null,
  );
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<MemoryFormValues>();

  /** sourceAgentId -> 智能体名称 */
  const agentNameById = useMemo(() => {
    const map = new Map<string, string>();
    for (const agent of agents) {
      map.set(agent.id, agent.name);
    }
    return map;
  }, [agents]);

  const openCreateModal = () => {
    setEditingMemory(null);
    form.setFieldsValue({
      memoryType: "PREFERENCE",
      content: "",
    });
    setModalOpen(true);
  };

  const openEditModal = (record: LongTermMemoryVO) => {
    setEditingMemory(record);
    form.setFieldsValue({
      memoryType: record.memoryType,
      content: record.content,
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingMemory(null);
    form.resetFields();
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      if (editingMemory) {
        await updateMemoryHandle(editingMemory.id, {
          memoryType: values.memoryType,
          content: values.content.trim(),
        });
        message.success("记忆已更新");
      } else {
        await createMemoryHandle({
          memoryType: values.memoryType,
          content: values.content.trim(),
        });
        message.success("记忆已创建");
      }
      closeModal();
    } catch (error) {
      if (error instanceof Error && error.message) {
        message.error(error.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const formatTime = (value?: string) => {
    if (!value) return "-";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN");
  };

  const columns: ColumnsType<LongTermMemoryVO> = [
    {
      title: "内容",
      dataIndex: "content",
      key: "content",
      ellipsis: true,
    },
    {
      title: "类型",
      dataIndex: "memoryType",
      key: "memoryType",
      width: 100,
      render: (type: LongTermMemoryType) => (
        <Tag color={type === "PREFERENCE" ? "purple" : "blue"}>
          {MEMORY_TYPE_LABEL[type] ?? type}
        </Tag>
      ),
    },
    {
      title: "来源智能体",
      dataIndex: "sourceAgentId",
      key: "sourceAgentId",
      width: 140,
      render: (agentId?: string) =>
        agentId
          ? agentNameById.get(agentId) ?? agentId
          : <Text type="secondary">-</Text>,
    },
    {
      title: "创建时间",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 180,
      render: (v: string) => formatTime(v),
    },
    {
      title: "操作",
      key: "action",
      width: 140,
      render: (_: unknown, record: LongTermMemoryVO) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEditModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除这条记忆吗？"
            description="删除后无法恢复，对话中将不再召回"
            onConfirm={() => deleteMemoryHandle(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="flex flex-col h-full p-6 overflow-y-auto">
      <div className="max-w-6xl w-full mx-auto">
        <Card className="mb-4">
          <div className="flex items-start gap-4">
            <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-amber-200 to-orange-200 flex items-center justify-center text-3xl shrink-0">
              <BulbOutlined />
            </div>
            <div className="flex-1">
              <Title level={3} className="mb-2">
                我的记忆
              </Title>
              <Paragraph className="text-gray-600 mb-0">
                在此管理你的偏好与事实记忆。记忆按用户隔离，在所有智能体对话中均可被召回。
              </Paragraph>
            </div>
          </div>
        </Card>

        <Card
          title={`记忆列表 (${memories.length})`}
          extra={
            <Space>
              <Select
                allowClear
                placeholder="按类型筛选"
                style={{ width: 140 }}
                value={typeFilter}
                onChange={(v) => setTypeFilter(v)}
                options={MEMORY_TYPE_OPTIONS}
              />
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={openCreateModal}
              >
                新增记忆
              </Button>
            </Space>
          }
        >
          {loading ? (
            <div className="text-center py-8">
              <Text type="secondary">加载中...</Text>
            </div>
          ) : memories.length === 0 ? (
            <Empty description={<Text type="secondary">暂无记忆</Text>} />
          ) : (
            <Table
              columns={columns}
              dataSource={memories}
              rowKey="id"
              pagination={{
                pageSize: 10,
                showTotal: (total) => `共 ${total} 条`,
              }}
            />
          )}
        </Card>
      </div>

      <Modal
        title={editingMemory ? "编辑记忆" : "新增记忆"}
        open={modalOpen}
        onCancel={closeModal}
        onOk={handleSubmit}
        confirmLoading={submitting}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" className="mt-4">
          <Form.Item
            name="memoryType"
            label="类型"
            rules={[{ required: true, message: "请选择记忆类型" }]}
          >
            <Select options={MEMORY_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
            rules={[
              { required: true, message: "请输入记忆内容" },
              { whitespace: true, message: "记忆内容不能为空" },
            ]}
          >
            <Input.TextArea
              rows={4}
              placeholder="例如：请叫我小明 / 我住在上海"
              maxLength={300}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default MemoryView;
