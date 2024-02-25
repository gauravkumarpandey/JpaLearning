package bootstrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.testing.transaction.TransactionUtil.JPATransactionVoidFunction;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.spi.PersistenceUnitInfo;
import util.PersistenceUnitInfoImpl;
import util.provider.DataSourceProvider;
import util.provider.Database;

public abstract class AbstractJPAProgrammaticBootstrapTest {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	private EntityManagerFactory entityManagerFactory;

	public EntityManagerFactory entityManagerFactory() {
		return this.entityManagerFactory;
	}

	@Before
	public void init() {
		PersistenceUnitInfo persistenceUnitInfo = this.persistenceUnitInfo(this.getClass().getSimpleName());

		Map<String, Object> configuration = new HashMap<>();

		Integrator integrator = this.integrator();
		if (integrator != null) {
			configuration.put("hibernate.integrator_provider",
					(IntegratorProvider) () -> Collections.singletonList(integrator));
		}

		entityManagerFactory = new HibernatePersistenceProvider()
				.createContainerEntityManagerFactory(persistenceUnitInfo, configuration);
	}

	@After
	public void destroy() {
		entityManagerFactory.close();
	}

	protected abstract Class<?>[] entities();

	protected PersistenceUnitInfo persistenceUnitInfo(String name) {
		return new PersistenceUnitInfoImpl(name, this.entityClassNames(), this.properties());
	}

	protected Properties properties() {
		Properties properties = new Properties();
		DataSource dataSource = this.newDataSource();
		if (dataSource != null) {
			properties.put("hibernate.connection.datasource", dataSource);
		}
		properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());

		properties.put("hibernate.hbm2ddl.auto", "create-drop");
		return properties;
	}

	protected List<String> entityClassNames() {
		return Arrays.asList(this.entities()).stream().map(Class::getName).collect(Collectors.toList());
	}

	protected DataSource newDataSource() {
		return this.dataSourceProvider().dataSource();
	}

	protected DataSourceProvider dataSourceProvider() {
		return this.database().dataSourceProvider();
	}

	protected Database database() {
		return Database.HSQLDB;
	}

	protected Integrator integrator() {
		return null;
	}

	protected void doInJPA(JPATransactionVoidFunction function) {
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();
			function.beforeTransactionCompletion();
			txn = entityManager.getTransaction();
			txn.begin();
			function.accept(entityManager);
			if (!txn.getRollbackOnly()) {
				txn.commit();
			} else {
				try {
					txn.rollback();
				} catch (Exception e) {
					LOGGER.error("Rollback failure", e);
				}
			}
		} catch (Throwable t) {
			if (txn != null && txn.isActive()) {
				try {
					txn.rollback();
				} catch (Exception e) {
					LOGGER.error("Rollback failure", e);
				}
			}
			throw t;
		} finally {
			function.afterTransactionCompletion();
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}
}
