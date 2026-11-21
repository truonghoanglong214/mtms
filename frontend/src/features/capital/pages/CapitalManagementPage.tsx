import React, { useState, useEffect, useCallback } from 'react';
import {
  Coins,
  DollarSign,
  HandCoins,
  Users,
  ShieldAlert,
  FileCheck2,
  AlertCircle,
  Building2,
  RefreshCw
} from 'lucide-react';
import { useAuthStore } from '@/features/auth/authStore';
import { apiClient } from '@/shared/api/apiClient';
import {
  CapitalCommitment,
  CapitalSummary,
  Investor,
  InvestorAgreement,
  ProfitEntitlementTerm
} from '../types/capital.types';
import { capitalApi } from '../api/capitalApi';
import { InvestorListTab } from '../components/InvestorListTab';
import { CommitmentsProgressTab } from '../components/CommitmentsProgressTab';
import { ProfitTermsViewerTab } from '../components/ProfitTermsViewerTab';
import { CreateInvestorModal } from '../components/CreateInvestorModal';
import { CreateAgreementModal } from '../components/CreateAgreementModal';
import { CreateCommitmentModal } from '../components/CreateCommitmentModal';
import { RecordContributionModal } from '../components/RecordContributionModal';
import { CreateProfitTermModal } from '../components/CreateProfitTermModal';

interface BranchOption {
  id: string;
  name: string;
  businessEntityId?: string;
  code?: string;
}

export const CapitalManagementPage: React.FC = () => {
  const activeBranchId = useAuthStore((state) => state.activeBranchId);
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch);
  const user = useAuthStore((state) => state.user);

  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '');

  const [activeTab, setActiveTab] = useState<'commitments' | 'investors' | 'terms'>('commitments');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [summary, setSummary] = useState<CapitalSummary | null>(null);
  const [investors, setInvestors] = useState<Investor[]>([]);
  const [agreements, setAgreements] = useState<InvestorAgreement[]>([]);
  const [commitments, setCommitments] = useState<CapitalCommitment[]>([]);
  const [profitTerms, setProfitTerms] = useState<ProfitEntitlementTerm[]>([]);

  // Modals state
  const [isInvestorModalOpen, setIsInvestorModalOpen] = useState(false);
  const [isAgreementModalOpen, setIsAgreementModalOpen] = useState(false);
  const [isCommitmentModalOpen, setIsCommitmentModalOpen] = useState(false);
  const [isContributionModalOpen, setIsContributionModalOpen] = useState(false);
  const [isProfitTermModalOpen, setIsProfitTermModalOpen] = useState(false);
  const [selectedCommitment, setSelectedCommitment] = useState<CapitalCommitment | null>(null);

  // Fetch branches
  useEffect(() => {
    const fetchBranches = async () => {
      try {
        const res = await apiClient.get<BranchOption[]>('/branches');
        const list = Array.isArray(res.data) ? res.data : (res.data as unknown as { data: BranchOption[] })?.data || [];
        setBranches(list);
        if (list.length > 0 && !selectedBranchId) {
          const initial = activeBranchId || list[0].id;
          const found = list.find((b: BranchOption) => b.id === initial);
          setSelectedBranchId(initial);
          setActiveBranch(initial, found?.name || null);
        }
      } catch (err) {
        console.error('Failed to load branches:', err);
      }
    };
    fetchBranches();
  }, [activeBranchId, selectedBranchId, setActiveBranch]);

  const selectedBranch = branches.find((b) => b.id === selectedBranchId);
  const entityId =
    selectedBranch?.businessEntityId ||
    user?.grants?.[0]?.businessEntityId ||
    '00000000-0000-0000-0000-000000000001';

  const loadData = useCallback(async () => {
    if (!selectedBranchId) return;
    setLoading(true);
    setError(null);
    try {
      const [sumRes, invRes, agrRes, comRes, termsRes] = await Promise.all([
        capitalApi.getCapitalSummary(selectedBranchId),
        capitalApi.getInvestorsByEntity(entityId).catch(() => [] as Investor[]),
        capitalApi.getAgreementsByBranch(selectedBranchId),
        capitalApi.getCommitmentsByBranch(selectedBranchId),
        capitalApi.getProfitTermsByBranch(selectedBranchId)
      ]);

      setSummary(sumRes);
      setInvestors(invRes);
      setAgreements(agrRes);
      setCommitments(comRes);
      setProfitTerms(termsRes);
    } catch (err: unknown) {
      console.error('Failed to load capital data:', err);
      const message = err instanceof Error ? err.message : 'Lỗi khi tải dữ liệu vốn và cam kết góp vốn';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [selectedBranchId, entityId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleBranchChange = (newBranchId: string) => {
    setSelectedBranchId(newBranchId);
    const found = branches.find((b) => b.id === newBranchId);
    setActiveBranch(newBranchId, found?.name || null);
  };

  const handleOpenRecordContribution = (commitment: CapitalCommitment) => {
    setSelectedCommitment(commitment);
    setIsContributionModalOpen(true);
  };

  const formatVnd = (val?: number) => {
    if (val === undefined || val === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div className="space-y-6 p-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Coins className="h-6 w-6 text-primary-600" />
            <h1 className="text-2xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
              Quản lý Vốn góp & Cổ đông
            </h1>
          </div>
          <p className="mt-1 text-sm text-neutral-500">
            Theo dõi cam kết góp vốn, tiến độ thu tiền thực tế và tỷ lệ hưởng lợi nhuận tham chiếu
          </p>
        </div>

        {/* Branch Selector & Refresh */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 shadow-sm dark:border-neutral-700 dark:bg-neutral-800">
            <Building2 className="h-4 w-4 text-neutral-400" />
            <select
              value={selectedBranchId}
              onChange={(e) => handleBranchChange(e.target.value)}
              className="bg-transparent text-sm font-medium focus:outline-none dark:text-neutral-200"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.code || 'BR'})
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={() => loadData()}
            disabled={loading}
            className="flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-medium text-neutral-700 shadow-sm hover:bg-neutral-50 disabled:opacity-50 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-300 dark:hover:bg-neutral-700"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>
      </div>

      {/* G5 Gate Disclaimer Banner */}
      <div className="flex items-center gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 shadow-sm dark:border-amber-900 dark:bg-amber-950/40">
        <ShieldAlert className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
        <div className="text-xs text-amber-900 dark:text-amber-200">
          <span className="font-semibold">GIỚI HẠN GIAI ĐOẠN HIỆN TẠI:</span> Dữ liệu tỷ lệ chia lợi nhuận hiển thị dưới đây là thông tin thiết lập tham chiếu nội bộ. Hệ thống không thực hiện chi trả lợi nhuận thực tế cho cổ đông trong Pha 0.
        </div>
      </div>

      {/* Error notification */}
      {error && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Metric Stat Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Card 1: Committed */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Tổng Vốn Cam Kết</span>
            <div className="rounded-lg bg-blue-100 p-2 text-blue-600 dark:bg-blue-950 dark:text-blue-400">
              <DollarSign className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
              {formatVnd(summary?.totalCommittedValue)}
            </div>
            <p className="mt-1 text-xs text-neutral-500">
              {commitments.length} cam kết góp vốn
            </p>
          </div>
        </div>

        {/* Card 2: Contributed */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Vốn Thực Góp (Tiền mặt/TS)</span>
            <div className="rounded-lg bg-emerald-100 p-2 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-400">
              <HandCoins className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
              {formatVnd(summary?.totalActualContributed)}
            </div>
            <div className="mt-1 flex items-center gap-1.5 text-xs text-neutral-500">
              <div className="h-2 w-24 overflow-hidden rounded-full bg-neutral-200 dark:bg-neutral-700">
                <div
                  className="h-full bg-emerald-500"
                  style={{
                    width: `${Math.min(
                      100,
                      summary && summary.totalCommittedValue > 0
                        ? (summary.totalActualContributed / summary.totalCommittedValue) * 100
                        : 0
                    )}%`
                  }}
                />
              </div>
              <span>
                {summary && summary.totalCommittedValue > 0
                  ? ((summary.totalActualContributed / summary.totalCommittedValue) * 100).toFixed(1)
                  : 0}
                %
              </span>
            </div>
          </div>
        </div>

        {/* Card 3: Outstanding (TC-032 Invariant) */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Vốn Còn Phải Thu</span>
            <div className="rounded-lg bg-amber-100 p-2 text-amber-600 dark:bg-amber-950 dark:text-amber-400">
              <Coins className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-amber-600 dark:text-amber-400">
              {formatVnd(summary?.totalOutstandingValue)}
            </div>
            <p className="mt-1 text-xs text-neutral-500">
              Phải thu = Cam kết - Thực góp
            </p>
          </div>
        </div>

        {/* Card 4: Investors */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Cổ đông / Hợp đồng</span>
            <div className="rounded-lg bg-indigo-100 p-2 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400">
              <Users className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
              {summary?.investorCount || 0}
            </div>
            <p className="mt-1 text-xs text-neutral-500">
              {agreements.length} thỏa thuận đang quản lý
            </p>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex border-b border-neutral-200 dark:border-neutral-800">
        <button
          onClick={() => setActiveTab('commitments')}
          className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
            activeTab === 'commitments'
              ? 'border-primary-600 text-primary-600 dark:border-primary-400 dark:text-primary-400'
              : 'border-transparent text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200'
          }`}
        >
          <HandCoins className="h-4 w-4" />
          Tiến độ góp vốn
          <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
            {commitments.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('investors')}
          className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
            activeTab === 'investors'
              ? 'border-primary-600 text-primary-600 dark:border-primary-400 dark:text-primary-400'
              : 'border-transparent text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200'
          }`}
        >
          <Users className="h-4 w-4" />
          Danh sách Cổ đông & Thỏa thuận
          <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
            {agreements.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('terms')}
          className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
            activeTab === 'terms'
              ? 'border-primary-600 text-primary-600 dark:border-primary-400 dark:text-primary-400'
              : 'border-transparent text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200'
          }`}
        >
          <FileCheck2 className="h-4 w-4" />
          Tỷ lệ hưởng Lợi nhuận
          <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
            {profitTerms.length}
          </span>
        </button>
      </div>

      {/* Tab Panels */}
      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <div className="flex items-center gap-2 text-sm text-neutral-500">
            <RefreshCw className="h-5 w-5 animate-spin" />
            Đang tải dữ liệu vốn...
          </div>
        </div>
      ) : (
        <>
          {activeTab === 'commitments' && (
            <CommitmentsProgressTab
              commitments={commitments}
              onOpenCreateCommitment={() => setIsCommitmentModalOpen(true)}
              onOpenRecordContribution={handleOpenRecordContribution}
            />
          )}

          {activeTab === 'investors' && (
            <InvestorListTab
              investors={investors}
              agreements={agreements}
              onOpenCreateInvestor={() => setIsInvestorModalOpen(true)}
              onOpenCreateAgreement={() => setIsAgreementModalOpen(true)}
            />
          )}

          {activeTab === 'terms' && (
            <ProfitTermsViewerTab
              profitTerms={profitTerms}
              breakdowns={summary?.investorBreakdowns || []}
              onOpenCreateProfitTerm={() => setIsProfitTermModalOpen(true)}
            />
          )}
        </>
      )}

      {/* Modals */}
      <CreateInvestorModal
        entityId={entityId}
        isOpen={isInvestorModalOpen}
        onClose={() => setIsInvestorModalOpen(false)}
        onSuccess={() => {
          setIsInvestorModalOpen(false);
          loadData();
        }}
      />

      <CreateAgreementModal
        branchId={selectedBranchId}
        investors={investors}
        isOpen={isAgreementModalOpen}
        onClose={() => setIsAgreementModalOpen(false)}
        onSuccess={() => {
          setIsAgreementModalOpen(false);
          loadData();
        }}
      />

      <CreateCommitmentModal
        branchId={selectedBranchId}
        agreements={agreements}
        isOpen={isCommitmentModalOpen}
        onClose={() => setIsCommitmentModalOpen(false)}
        onSuccess={() => {
          setIsCommitmentModalOpen(false);
          loadData();
        }}
      />

      <RecordContributionModal
        branchId={selectedBranchId}
        commitment={selectedCommitment}
        isOpen={isContributionModalOpen}
        onClose={() => {
          setIsContributionModalOpen(false);
          setSelectedCommitment(null);
        }}
        onSuccess={() => {
          setIsContributionModalOpen(false);
          setSelectedCommitment(null);
          loadData();
        }}
      />

      <CreateProfitTermModal
        branchId={selectedBranchId}
        agreements={agreements}
        isOpen={isProfitTermModalOpen}
        onClose={() => setIsProfitTermModalOpen(false)}
        onSuccess={() => {
          setIsProfitTermModalOpen(false);
          loadData();
        }}
      />
    </div>
  );
};
