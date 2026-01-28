// Shared domain models aligned with backend
export type Currency = 'USD' | 'EUR' | 'GBP' | 'TRY';

export type WalletStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

export type TransactionDirection = 'CREDIT' | 'DEBIT';

export type TransactionGroupType = 
  | 'USER_ACTION' 
  | 'SYSTEM_ADJUSTMENT' 
  | 'FX_CONVERSION' 
  | 'PAYMENT' 
  | 'REVERSAL' 
  | 'FEE';

export type ReferenceType = 
  | 'DEPOSIT' 
  | 'WITHDRAWAL' 
  | 'TRANSFER' 
  | 'FX_EXCHANGE' 
  | 'CARD_PAYMENT';

export interface Money {
  amount: number;
  currency: Currency;
}

export interface Wallet {
  id: string;
  ownerId: string;
  baseCurrency: Currency;
  status: WalletStatus;
  createdAt: string;
}

export interface Balance {
  walletId: string;
  balance: Money;
  calculatedAt: string;
}

export interface Transaction {
  id: string;
  walletId: string;
  amount: Money;
  direction: TransactionDirection;
  groupType: TransactionGroupType;
  referenceType: ReferenceType;
  referenceId: string | null;
  description: string | null;
  occurredAt: string;
}

// Request DTOs
export interface CreateWalletRequest {
  baseCurrency: Currency;
}

export interface DepositRequest {
  walletId: string;
  amount: number;
  currency: Currency;
  idempotencyKey?: string;
}

export interface WithdrawalRequest {
  walletId: string;
  amount: number;
  currency: Currency;
  idempotencyKey?: string;
}

export interface TransferRequest {
  sourceWalletId: string;
  targetWalletId: string;
  amount: number;
  currency: Currency;
  description?: string;
  idempotencyKey?: string;
}

export interface FxConvertRequest {
  sourceWalletId: string;
  targetWalletId: string;
  amount: number;
  sourceCurrency: Currency;
  idempotencyKey?: string;
}

// Response DTOs
export interface DepositResponse {
  depositId: string;
  transactionId: string;
  walletId: string;
  amount: Money;
  status: string;
  processedAt: string;
}

export interface WithdrawalResponse {
  withdrawalId: string;
  transactionId: string;
  walletId: string;
  amount: Money;
  status: string;
  processedAt: string;
}

export interface TransferResponse {
  transferId: string;
  sourceTransactionId: string;
  targetTransactionId: string;
  sourceWalletId: string;
  targetWalletId: string;
  amount: Money;
  status: string;
  processedAt: string;
}

export interface FxConvertResponse {
  conversionId: string;
  debitTransactionId: string;
  creditTransactionId: string;
  sourceAmount: Money;
  targetAmount: Money;
  exchangeRate: number;
  processedAt: string;
}

export interface FxRateResponse {
  fromCurrency: Currency;
  toCurrency: Currency;
  rate: number;
  sourceAmount: number;
  targetAmount: number;
}

// Pagination
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
