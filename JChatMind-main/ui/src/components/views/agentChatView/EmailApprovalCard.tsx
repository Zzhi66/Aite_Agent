import { useEffect, useState } from "react";
import { Alert, Button, Card, Space, Spin } from "antd";
import { get, post } from "../../../api/http.ts";

type Status = "PENDING" | "SENDING" | "SENT" | "CANCELLED" | "EXPIRED" | "FAILED";
interface EmailApproval {
  id: string;
  to: string;
  subject: string;
  content: string;
  expiresAt: string;
  status: Status;
}

const labels: Record<Status, string> = {
  PENDING: "邮件尚未发送，请核对后确认。",
  SENDING: "发送处理中。若长时间未更新，可能是发送结果尚未同步，请核对发件箱，勿重复发起。",
  SENT: "邮件已提交至邮件服务器。",
  CANCELLED: "已取消，邮件未发送。",
  EXPIRED: "确认已过期，邮件未发送。请重新发起。",
  FAILED: "发送失败或结果未知，请先核对发件箱，再决定是否重新发起。",
};

export default function EmailApprovalCard({ approvalId }: { approvalId: string }) {
  const [approval, setApproval] = useState<EmailApproval | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const status = approval?.status;

  useEffect(() => {
    let active = true;
    get<EmailApproval>(`/email-approvals/${encodeURIComponent(approvalId)}`)
      .then((value) => { if (active) setApproval(value); })
      .catch(() => { if (active) setError("无法读取确认请求，请刷新重试或检查登录状态。"); });
    return () => { active = false; };
  }, [approvalId]);

  // Refresh while pending/sending: reflects expiry and actions from another browser tab.
  useEffect(() => {
    if (busy || !status || !["PENDING", "SENDING"].includes(status)) return;
    let active = true;
    const timer = window.setInterval(() => {
      get<EmailApproval>(`/email-approvals/${encodeURIComponent(approvalId)}`)
        .then((value) => { if (active) setApproval(value); })
        .catch(() => { /* Explicit actions still display network errors. */ });
    }, 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [status, approvalId, busy]);

  const decide = async (action: "confirm" | "cancel") => {
    setBusy(true);
    setError("");
    try {
      // Send only the ID; recipients and contents come from the immutable server-side draft.
      setApproval(await post<EmailApproval>(`/email-approvals/${encodeURIComponent(approvalId)}/${action}`));
    } catch {
      setError("操作结果暂未确认，请刷新查看状态；重复确认不会再次触发同一封邮件。");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card size="small" title="邮件发送确认" className="my-2 w-full max-w-xl">
      {error && <Alert type="warning" title={error} className="mb-3" />}
      {!approval && !error && <Spin size="small" />}
      {approval && <>
        <p className="break-all"><strong>收件人：</strong>{approval.to}</p>
        <p className="break-words"><strong>主题：</strong>{approval.subject}</p>
        <div className="my-3 max-h-64 overflow-y-auto whitespace-pre-wrap break-words rounded bg-gray-50 p-3">
          {approval.content}
        </div>
        <p role="status" className="mb-2">{labels[approval.status]}</p>
        {approval.status === "PENDING" && <>
          <p className="mb-3 text-xs text-gray-500">有效期至 {new Date(approval.expiresAt).toLocaleTimeString()}</p>
          <Space>
            <Button type="primary" loading={busy} onClick={() => void decide("confirm")}>确认发送</Button>
            <Button disabled={busy} onClick={() => void decide("cancel")}>取消</Button>
          </Space>
        </>}
      </>}
    </Card>
  );
}
