import { useCallback, useEffect, useState } from "react";
import {
  createLongTermMemory,
  deleteLongTermMemory,
  getLongTermMemories,
  updateLongTermMemory,
  type CreateLongTermMemoryRequest,
  type LongTermMemoryType,
  type LongTermMemoryVO,
  type UpdateLongTermMemoryRequest,
} from "../api/api.ts";

/**
 * 用户级长期记忆 Hook：跨 Agent 列表，支持类型筛选与 CRUD
 */
export function useLongTermMemories(memoryType?: LongTermMemoryType) {
  const [memories, setMemories] = useState<LongTermMemoryVO[]>([]);
  const [loading, setLoading] = useState(false);

  const refreshMemories = useCallback(async () => {
    setLoading(true);
    try {
      const resp = await getLongTermMemories(memoryType);
      setMemories(resp.memories ?? []);
    } finally {
      setLoading(false);
    }
  }, [memoryType]);

  useEffect(() => {
    refreshMemories().then();
  }, [refreshMemories]);

  async function createMemoryHandle(request: CreateLongTermMemoryRequest) {
    await createLongTermMemory(request);
    await refreshMemories();
  }

  async function updateMemoryHandle(
    memoryId: string,
    request: UpdateLongTermMemoryRequest,
  ) {
    await updateLongTermMemory(memoryId, request);
    await refreshMemories();
  }

  async function deleteMemoryHandle(memoryId: string) {
    await deleteLongTermMemory(memoryId);
    await refreshMemories();
  }

  return {
    memories,
    loading,
    refreshMemories,
    createMemoryHandle,
    updateMemoryHandle,
    deleteMemoryHandle,
  };
}
