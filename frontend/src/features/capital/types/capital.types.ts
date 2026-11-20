export type ContributionType = 'CASH' | 'ASSET' | 'WORK' | 'MIXED';
export type AgreementStatus = 'ACTIVE' | 'TERMINATED' | 'EXPIRED';
export type CommitmentStatus = 'PENDING' | 'PARTIALLY_FULFILLED' | 'FULFILLED' | 'CANCELLED';

export interface Investor {
  id: string;
  entityId: string;
  displayName: string;
  contactPhone?: string;
  contactEmail?: string;
  identityNumber?: string;
  contactNote?: string;
  active: boolean;
  createdAt: string;
}

export interface InvestorAgreement {
  id: string;
  investorId: string;
  investorName: string;
  branchId: string;
  agreementRef: string;
  contributionType: ContributionType;
  effectiveFrom: string;
  effectiveTo?: string;
  status: AgreementStatus;
  eligibilityTerms?: string;
  evidenceFileId?: string;
  createdAt: string;
}

export interface CapitalCommitment {
  id: string;
  agreementId: string;
  investorId?: string;
  investorName: string;
  agreementRef: string;
  committedValue: number;
  recognizedNonCashValue: number;
  actualContributedValue: number;
  outstandingValue: number;
  commitmentDueDate?: string;
  status: CommitmentStatus;
  notes?: string;
  createdAt: string;
}

export interface CapitalContribution {
  id: string;
  commitmentId: string;
  contributedOn: string;
  contributionType: ContributionType;
  recognizedValue: number;
  accountId?: string;
  accountName?: string;
  assetId?: string;
  ledgerEntryId?: string;
  evidenceFileId?: string;
  notes?: string;
  createdBy: string;
  createdAt: string;
}

export interface ProfitEntitlementTerm {
  id: string;
  agreementId: string;
  investorId?: string;
  investorName: string;
  agreementRef: string;
  effectiveFrom: string;
  effectiveTo?: string;
  profitShareRatio: number;
  profitSharePercent: number;
  eligibilityConditions?: string;
  approvedBy: string;
  evidenceFileId?: string;
  createdAt: string;
  isInformationalOnly: boolean;
  disclaimer: string;
}

export interface InvestorCapitalBreakdown {
  investorId: string;
  investorName: string;
  totalCommittedValue: number;
  totalContributedValue: number;
  outstandingValue: number;
  capitalSharePercent: number;
  profitSharePercent: number;
  isDifferentRatio: boolean;
}

export interface CapitalSummary {
  branchId: string;
  totalCommittedValue: number;
  totalActualContributed: number;
  totalOutstandingValue: number;
  investorCount: number;
  agreementCount: number;
  isInformationalOnly: boolean;
  disclaimer: string;
  investorBreakdowns: InvestorCapitalBreakdown[];
}

export interface CreateInvestorRequest {
  displayName: string;
  contactPhone?: string;
  contactEmail?: string;
  identityNumber?: string;
  contactNote?: string;
}

export interface CreateAgreementRequest {
  investorId: string;
  agreementRef: string;
  contributionType: ContributionType;
  effectiveFrom: string;
  effectiveTo?: string;
  eligibilityTerms?: string;
  evidenceFileId?: string;
}

export interface CreateCommitmentRequest {
  agreementId: string;
  committedValue: number;
  recognizedNonCashValue?: number;
  commitmentDueDate?: string;
  notes?: string;
}

export interface RecordContributionRequest {
  contributedOn: string;
  contributionType: ContributionType;
  recognizedValue: number;
  accountId?: string;
  assetId?: string;
  evidenceFileId?: string;
  notes?: string;
}

export interface CreateProfitTermRequest {
  agreementId: string;
  effectiveFrom: string;
  effectiveTo?: string;
  profitShareRatio: number;
  eligibilityConditions?: string;
  evidenceFileId?: string;
}

export const CONTRIBUTION_TYPE_LABELS: Record<ContributionType, string> = {
  CASH: 'Tiền mặt / Chuyển khoản',
  ASSET: 'Tài sản / Thiết bị',
  WORK: 'Công sức / Trí tuệ',
  MIXED: 'Hỗn hợp'
};

export const AGREEMENT_STATUS_LABELS: Record<AgreementStatus, { label: string; color: string }> = {
  ACTIVE: { label: 'Đang hiệu lực', color: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300' },
  TERMINATED: { label: 'Đã chấm dứt', color: 'bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300' },
  EXPIRED: { label: 'Hết hạn', color: 'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-300' }
};

export const COMMITMENT_STATUS_LABELS: Record<CommitmentStatus, { label: string; color: string }> = {
  PENDING: { label: 'Chờ góp vốn', color: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300' },
  PARTIALLY_FULFILLED: { label: 'Đang góp dở dang', color: 'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300' },
  FULFILLED: { label: 'Đã hoàn tất 100%', color: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-300' }
};
