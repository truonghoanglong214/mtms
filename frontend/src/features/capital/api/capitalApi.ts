import { apiClient } from '@/shared/api/apiClient';
import {
  CapitalCommitment,
  CapitalContribution,
  CapitalSummary,
  CreateAgreementRequest,
  CreateCommitmentRequest,
  CreateInvestorRequest,
  CreateProfitTermRequest,
  Investor,
  InvestorAgreement,
  ProfitEntitlementTerm,
  RecordContributionRequest
} from '../types/capital.types';

export const capitalApi = {
  // Investors (Entity Scoped)
  getInvestorsByEntity: async (entityId: string): Promise<Investor[]> => {
    const res = await apiClient.get<Investor[]>(`/entities/${entityId}/investors`);
    return res.data;
  },

  registerInvestor: async (entityId: string, payload: CreateInvestorRequest): Promise<Investor> => {
    const res = await apiClient.post<Investor>(`/entities/${entityId}/investors`, payload);
    return res.data;
  },

  // Agreements (Branch Scoped)
  getAgreementsByBranch: async (branchId: string): Promise<InvestorAgreement[]> => {
    const res = await apiClient.get<InvestorAgreement[]>(`/branches/${branchId}/capital/agreements`);
    return res.data;
  },

  createAgreement: async (branchId: string, payload: CreateAgreementRequest): Promise<InvestorAgreement> => {
    const res = await apiClient.post<InvestorAgreement>(`/branches/${branchId}/capital/agreements`, payload);
    return res.data;
  },

  // Commitments (Branch Scoped)
  getCommitmentsByBranch: async (branchId: string): Promise<CapitalCommitment[]> => {
    const res = await apiClient.get<CapitalCommitment[]>(`/branches/${branchId}/capital/commitments`);
    return res.data;
  },

  createCommitment: async (branchId: string, payload: CreateCommitmentRequest): Promise<CapitalCommitment> => {
    const res = await apiClient.post<CapitalCommitment>(`/branches/${branchId}/capital/commitments`, payload);
    return res.data;
  },

  // Contributions (Branch Scoped)
  recordContribution: async (
    branchId: string,
    commitmentId: string,
    payload: RecordContributionRequest
  ): Promise<CapitalContribution> => {
    const res = await apiClient.post<CapitalContribution>(
      `/branches/${branchId}/capital/commitments/${commitmentId}/contributions`,
      payload
    );
    return res.data;
  },

  getContributionsByBranch: async (branchId: string): Promise<CapitalContribution[]> => {
    const res = await apiClient.get<CapitalContribution[]>(`/branches/${branchId}/capital/contributions`);
    return res.data;
  },

  // Profit Terms (Branch Scoped)
  getProfitTermsByBranch: async (branchId: string): Promise<ProfitEntitlementTerm[]> => {
    const res = await apiClient.get<ProfitEntitlementTerm[]>(`/branches/${branchId}/capital/profit-terms`);
    return res.data;
  },

  configureProfitTerms: async (
    branchId: string,
    payload: CreateProfitTermRequest
  ): Promise<ProfitEntitlementTerm> => {
    const res = await apiClient.post<ProfitEntitlementTerm>(`/branches/${branchId}/capital/profit-terms`, payload);
    return res.data;
  },

  // Capital Summary & TC-032 / CAP-INV-01
  getCapitalSummary: async (branchId: string): Promise<CapitalSummary> => {
    const res = await apiClient.get<CapitalSummary>(`/branches/${branchId}/capital/summary`);
    return res.data;
  }
};
