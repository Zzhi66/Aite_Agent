import React, { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Space,
  Switch,
  Typography,
  message,
} from "antd";
import { MailOutlined, SendOutlined } from "@ant-design/icons";
import {
  getUserMailConfig,
  saveUserMailConfig,
  sendUserMailConfigTest,
  type SaveUserMailConfigRequest,
  type UserMailConfigVO,
} from "../../api/api.ts";

const { Title, Paragraph, Text } = Typography;

interface EmailFormValues {
  fromEmail: string;
  smtpHost: string;
  smtpPort: number;
  smtpPassword?: string;
  useSsl: boolean;
}

/**
 * 用户个人发件邮箱配置页（每人使用自己的 SMTP 发信）
 */
const EmailSettingsView: React.FC = () => {
  const [form] = Form.useForm<EmailFormValues>();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [config, setConfig] = useState<UserMailConfigVO | null>(null);

  const loadConfig = useCallback(async () => {
    setLoading(true);
    try {
      const resp = await getUserMailConfig();
      const c = resp.config;
      setConfig(c);
      if (c.configured) {
        form.setFieldsValue({
          fromEmail: c.fromEmail,
          smtpHost: c.smtpHost ?? "smtp.qq.com",
          smtpPort: c.smtpPort ?? 465,
          useSsl: c.useSsl ?? true,
          smtpPassword: "",
        });
      } else {
        form.setFieldsValue({
          smtpHost: "smtp.qq.com",
          smtpPort: 587,
          useSsl: false,
        });
      }
    } catch {
      message.error("加载邮箱配置失败");
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    void loadConfig();
  }, [loadConfig]);

  const onSave = async (values: EmailFormValues) => {
    setSaving(true);
    try {
      const body: SaveUserMailConfigRequest = {
        fromEmail: values.fromEmail,
        smtpHost: values.smtpHost,
        smtpPort: values.smtpPort,
        useSsl: values.useSsl,
      };
      if (values.smtpPassword?.trim()) {
        body.smtpPassword = values.smtpPassword.trim();
      }
      await saveUserMailConfig(body);
      message.success("邮箱配置已保存");
      form.setFieldValue("smtpPassword", "");
      await loadConfig();
    } catch (e) {
      if (e instanceof Error && e.message) {
        message.error(e.message);
      }
    } finally {
      setSaving(false);
    }
  };

  const onTest = async () => {
    setTesting(true);
    try {
      await sendUserMailConfigTest();
      message.success("测试邮件已发送，请查收您的发件邮箱");
    } catch (e) {
      if (e instanceof Error && e.message) {
        message.error(e.message);
      }
    } finally {
      setTesting(false);
    }
  };

  return (
    <div className="h-full overflow-auto p-6 bg-gray-50">
      <div className="max-w-2xl mx-auto">
        <Title level={3}>
          <MailOutlined className="mr-2" />
          邮箱设置
        </Title>
        <Paragraph type="secondary">
          智能体发送邮件时将使用<strong>您本人</strong>配置的邮箱作为发件人。
          QQ 邮箱：网页版 → 设置 → 账户 → 开启 POP3/SMTP → 生成<strong>授权码</strong>（16 位，不是 QQ 登录密码）。
        </Paragraph>
        <Alert
          className="mb-4"
          type="info"
          showIcon
          message="推荐 SMTP 配置（QQ 邮箱）"
          description="服务器 smtp.qq.com；端口 587、关闭 SSL（STARTTLS）；或端口 465、开启 SSL。修改端口/SSL 后请重新填写授权码并保存，再发送测试邮件。"
        />

        {!loading && !config?.configured && (
          <Alert
            className="mb-4"
            type="warning"
            showIcon
            message="尚未配置发件邮箱"
            description="保存配置后，为智能体勾选 emailTool 即可在对话中代您发信。"
          />
        )}

        <Card loading={loading}>
          <Form
            form={form}
            layout="vertical"
            onFinish={onSave}
            initialValues={{ smtpHost: "smtp.qq.com", smtpPort: 587, useSsl: false }}
          >
            <Form.Item
              label="发件邮箱"
              name="fromEmail"
              rules={[
                { required: true, message: "请输入发件邮箱" },
                { type: "email", message: "邮箱格式不正确" },
              ]}
            >
              <Input placeholder="yourname@qq.com" />
            </Form.Item>

            <Form.Item
              label="SMTP 服务器"
              name="smtpHost"
              rules={[{ required: true, message: "请输入 SMTP 服务器" }]}
            >
              <Input placeholder="smtp.qq.com" />
            </Form.Item>

            <Form.Item
              label="SMTP 端口"
              name="smtpPort"
              rules={[{ required: true, message: "请输入端口" }]}
            >
              <InputNumber className="w-full" min={1} max={65535} />
            </Form.Item>

            <Form.Item label="使用 SSL" name="useSsl" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Text type="secondary" className="block -mt-3 mb-4">
              587 + 关闭 SSL：STARTTLS（推荐，与系统默认一致）；465 + 开启 SSL：隐式 SSL。
            </Text>

            <Form.Item
              label="SMTP 授权码"
              name="smtpPassword"
              rules={
                config?.configured
                  ? []
                  : [{ required: true, message: "首次配置请填写授权码" }]
              }
              extra={
                config?.passwordSet
                  ? "已保存授权码；留空表示不修改，填写则更新。"
                  : "请填写邮箱服务商提供的 SMTP 授权码"
              }
            >
              <Input.Password placeholder="授权码" autoComplete="new-password" />
            </Form.Item>

            <Space>
              <Button type="primary" htmlType="submit" loading={saving}>
                保存配置
              </Button>
              <Button
                icon={<SendOutlined />}
                onClick={onTest}
                loading={testing}
                disabled={!config?.configured}
              >
                发送测试邮件
              </Button>
            </Space>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default EmailSettingsView;
