package buerostack.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;

@Configuration
public class DatabaseConfig {

    /**
     * Custom JWT DataSource - restricted to custom_jwt schema
     */
    @Bean(name = "customJwtDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.custom-jwt")
    public DataSource customJwtDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Auth DataSource - restricted to auth schema
     */
    @Bean(name = "authDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.auth")
    public DataSource authDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Primary DataSource (for introspection and shared services)
     */
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Custom JWT Entity Manager Factory
     */
    @Primary
    @Bean(name = "customJwtEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean customJwtEntityManagerFactory(
            @Qualifier("customJwtDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("buerostack.jwt.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPersistenceUnitName("customJwtPU");

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.default_schema", "custom_jwt");
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        factory.setJpaProperties(jpaProperties);

        return factory;
    }

    /**
     * Auth Entity Manager Factory
     */
    @Bean(name = "authEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean authEntityManagerFactory(
            @Qualifier("authDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("buerostack.auth.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPersistenceUnitName("authPU");

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.default_schema", "auth");
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        factory.setJpaProperties(jpaProperties);

        return factory;
    }

    /**
     * Custom JWT Transaction Manager
     */
    @Primary
    @Bean(name = "customJwtTransactionManager")
    public PlatformTransactionManager customJwtTransactionManager(
            @Qualifier("customJwtEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /**
     * Auth Transaction Manager
     */
    @Bean(name = "authTransactionManager")
    public PlatformTransactionManager authTransactionManager(
            @Qualifier("authEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

/**
 * Custom JWT Repository Configuration
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "buerostack.jwt.repo",
    entityManagerFactoryRef = "customJwtEntityManagerFactory",
    transactionManagerRef = "customJwtTransactionManager"
)
class CustomJwtRepositoryConfig {
}

/**
 * Auth Repository Configuration
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "buerostack.auth.repo",
    entityManagerFactoryRef = "authEntityManagerFactory",
    transactionManagerRef = "authTransactionManager"
)
class AuthRepositoryConfig {
}