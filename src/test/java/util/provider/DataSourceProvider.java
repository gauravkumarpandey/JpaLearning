package util.provider;

import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.dialect.Dialect;

import util.ReflectionUtils;

public interface DataSourceProvider {

	enum IdentifierStrategy {
		IDENTITY,
		SEQUENCE
	}

	String hibernateDialect();

	DataSource dataSource();

	Class<? extends DataSource> dataSourceClassName();

	Properties dataSourceProperties();

	String url();

	String username();

	String password();

	Database database();
	
	default Class<? extends Dialect> hibernateDialectClass() {
		return ReflectionUtils.getClass(hibernateDialect());
	}
}