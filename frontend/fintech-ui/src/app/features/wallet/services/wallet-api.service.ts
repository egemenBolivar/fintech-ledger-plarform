import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Wallet,
  Balance,
  Transaction,
  CreateWalletRequest,
  DepositRequest,
  DepositResponse,
  WithdrawalRequest,
  WithdrawalResponse,
  TransferRequest,
  TransferResponse,
  FxConvertRequest,
  FxConvertResponse,
  FxRateResponse,
  Page,
  TransactionDirection,
  TransactionGroupType,
  ReferenceType,
  Currency
} from '../../../shared/models/api.models';

export interface TransactionFilter {
  direction?: TransactionDirection;
  groupType?: TransactionGroupType;
  referenceType?: ReferenceType;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class WalletApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  // Wallet operations
  createWallet(request: CreateWalletRequest): Observable<Wallet> {
    return this.http.post<Wallet>(`${this.baseUrl}/wallets`, request);
  }

  getWallets(): Observable<Wallet[]> {
    return this.http.get<Wallet[]>(`${this.baseUrl}/wallets`);
  }

  getWallet(id: string): Observable<Wallet> {
    return this.http.get<Wallet>(`${this.baseUrl}/wallets/${id}`);
  }

  getBalance(walletId: string): Observable<Balance> {
    return this.http.get<Balance>(`${this.baseUrl}/wallets/${walletId}/balance`);
  }

  suspendWallet(id: string): Observable<Wallet> {
    return this.http.patch<Wallet>(`${this.baseUrl}/wallets/${id}/suspend`, {});
  }

  activateWallet(id: string): Observable<Wallet> {
    return this.http.patch<Wallet>(`${this.baseUrl}/wallets/${id}/activate`, {});
  }

  // Transaction operations
  getTransactions(walletId: string, filter?: TransactionFilter): Observable<Page<Transaction>> {
    let params = new HttpParams();
    if (filter) {
      if (filter.direction) params = params.set('direction', filter.direction);
      if (filter.groupType) params = params.set('groupType', filter.groupType);
      if (filter.referenceType) params = params.set('referenceType', filter.referenceType);
      if (filter.from) params = params.set('from', filter.from);
      if (filter.to) params = params.set('to', filter.to);
      if (filter.page !== undefined) params = params.set('page', filter.page.toString());
      if (filter.size !== undefined) params = params.set('size', filter.size.toString());
      if (filter.sort) params = params.set('sort', filter.sort);
    }
    return this.http.get<Page<Transaction>>(`${this.baseUrl}/wallets/${walletId}/transactions`, { params });
  }

  getTransaction(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/transactions/${id}`);
  }

  // Use-case operations
  deposit(request: DepositRequest): Observable<DepositResponse> {
    return this.http.post<DepositResponse>(`${this.baseUrl}/deposits`, {
      ...request,
      idempotencyKey: request.idempotencyKey || crypto.randomUUID()
    });
  }

  withdraw(request: WithdrawalRequest): Observable<WithdrawalResponse> {
    return this.http.post<WithdrawalResponse>(`${this.baseUrl}/withdrawals`, {
      ...request,
      idempotencyKey: request.idempotencyKey || crypto.randomUUID()
    });
  }

  transfer(request: TransferRequest): Observable<TransferResponse> {
    return this.http.post<TransferResponse>(`${this.baseUrl}/transfers`, {
      ...request,
      idempotencyKey: request.idempotencyKey || crypto.randomUUID()
    });
  }

  fxConvert(request: FxConvertRequest): Observable<FxConvertResponse> {
    return this.http.post<FxConvertResponse>(`${this.baseUrl}/fx/convert`, {
      ...request,
      idempotencyKey: request.idempotencyKey || crypto.randomUUID()
    });
  }

  getFxRate(from: Currency, to: Currency, amount: number): Observable<FxRateResponse> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('amount', amount.toString());
    return this.http.get<FxRateResponse>(`${this.baseUrl}/fx/rate`, { params });
  }
}
