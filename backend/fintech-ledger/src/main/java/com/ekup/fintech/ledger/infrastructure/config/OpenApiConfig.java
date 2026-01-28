package com.ekup.fintech.ledger.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI fintechLedgerOpenAPI() {
		return new OpenAPI()
				.addServersItem(new Server().url("http://localhost:8080").description("Local development"))
				.info(new Info()
						.title("FinTech Ledger API")
						.description("""
								Multi-currency wallet platform with immutable ledger-based transactions.
								
								## Key Features
								- **Wallet Management**: Create, suspend, activate wallets
								- **Immutable Transactions**: Append-only ledger design
								- **Derived Balances**: Balance = SUM(transactions)
								- **Multi-Currency**: USD, EUR, GBP, TRY support
								- **FX Conversion**: Currency exchange with mock rates
								- **Idempotency**: Duplicate request prevention
								
								## Business Rules
								- Wallet balance cannot go negative
								- Suspended/closed wallets cannot transact
								- Same-wallet transfers are rejected
								- Currency must match wallet's base currency
								""")
						.version("1.0.0")
						.contact(new Contact()
								.name("FinTech Team")
								.email("fintech@ekup.com"))
						.license(new License()
								.name("MIT")
								.url("https://opensource.org/licenses/MIT")));
	}
}
