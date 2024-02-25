package util.provider;


import org.hibernate.dialect.Dialect;
import org.testcontainers.containers.*;

import util.ReflectionUtils;

import java.util.Collections;

public enum Database {
	HSQLDB {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return HSQLDBDataSourceProvider.class;
		}
	},	
	MYSQL {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return MySQLDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new MySQLContainer("mysql:8.0");
		}
	},
	;

	private JdbcDatabaseContainer container;

	public JdbcDatabaseContainer getContainer() {
		return container;
	}

	public DataSourceProvider dataSourceProvider() {
		return ReflectionUtils.newInstance(dataSourceProviderClass().getName());
	}
	
	public abstract Class<? extends DataSourceProvider> dataSourceProviderClass();

	public void initContainer(String username, String password) {
		container = (JdbcDatabaseContainer) newJdbcDatabaseContainer()
			.withReuse(true)
			.withEnv(Collections.singletonMap("ACCEPT_EULA", "Y"))
			.withTmpFs(Collections.singletonMap("/testtmpfs", "rw"));
		if(supportsDatabaseName()) {
			container.withDatabaseName(databaseName());
		}
		if(supportsCredentials()) {
			container.withUsername(username).withPassword(password);
		}
		container.start();
	}

	protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
		throw new UnsupportedOperationException(
			String.format(
				"The [%s] database was not configured to use Testcontainers!",
				name()
			)
		);
	}

	protected boolean supportsDatabaseName() {
		return true;
	}

	protected String databaseName() {
		return "high-performance-java-persistence";
	}

	protected boolean supportsCredentials() {
		return true;
	}

	public static Database of(Dialect dialect) {
		Class<? extends Dialect> dialectClass = dialect.getClass();
		for(Database database : values()) {
			if(database.dataSourceProvider().hibernateDialectClass().isAssignableFrom(dialectClass)) {
				return database;
			}
		}
		throw new UnsupportedOperationException(
			String.format(
				"The provided Dialect [%s] is not supported!",
				dialectClass
			)
		);
	}
}