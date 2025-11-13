package pe.edu.pucp.morapack.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración del DataSource para el esquema moraTravelSimulation (Secondary).
 * Este esquema contiene datos históricos (~2 años) específicamente para simulaciones.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "pe.edu.pucp.morapack.repository.simulation",
    entityManagerFactoryRef = "simulationEntityManagerFactory",
    transactionManagerRef = "simulationTransactionManager"
)
public class SimulationDataSourceConfig {

    /**
     * DataSource Simulation usando HikariCP
     * Carga propiedades directamente desde spring.datasource.simulation.*
     */
    @Bean(name = "simulationDataSource")
    @ConfigurationProperties("spring.datasource.simulation")
    public HikariDataSource simulationDataSource() {
        return new HikariDataSource();
    }

    /**
     * EntityManagerFactory para el esquema Simulation
     */
    @Bean(name = "simulationEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean simulationEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("simulationDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("pe.edu.pucp.morapack.model.simulation")
                .persistenceUnit("simulation")
                .properties(hibernateProperties())
                .build();
    }

    /**
     * TransactionManager para el esquema Simulation
     */
    @Bean(name = "simulationTransactionManager")
    public PlatformTransactionManager simulationTransactionManager(
            @Qualifier("simulationEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /**
     * Propiedades de Hibernate compartidas
     */
    private Map<String, Object> hibernateProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.implicit_naming_strategy",
                      "org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl");
        properties.put("hibernate.physical_naming_strategy",
                      "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        properties.put("hibernate.globally_quoted_identifiers", true);
        return properties;
    }
}
