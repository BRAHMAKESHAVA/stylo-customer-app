//package org.backend.appConfig;
//
//import org.backend.repository.PartnerRepository;
//import org.backend.repository.SalonImagesRepository;
//import org.backend.repository.SalonRepository;
//
//import jakarta.persistence.EntityManagerFactory;
//
//import com.zaxxer.hikari.HikariDataSource;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.FilterType;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import javax.sql.DataSource;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * LOCAL datasource (MySQL).
// * Handles Users plus every entity that is NOT present in prod:
// * Booking, Address, Coupon, Package, Payment, Category, SalonResource,
// * SalonService, NotificationDevice, Customer, Review, etc.
// *
// * This is marked @Primary so any bean elsewhere that autowires a plain
// * DataSource / EntityManagerFactory / PlatformTransactionManager without
// * a qualifier keeps working exactly as before (unchanged default).
// *
// * ddl-auto stays "update" here, same as the original single-datasource setup.
// */
//@Configuration
//@EnableJpaRepositories(
//        basePackages = "org.backend.repository",
//        entityManagerFactoryRef = "localEntityManagerFactory",
//        transactionManagerRef = "localTransactionManager",
//        excludeFilters = @ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {
//                        SalonRepository.class,
//                        SalonImagesRepository.class,
//                        PartnerRepository.class
//                }
//        )
//)
//public class LocalDataSourceConfig {
//
//    @Primary
//    @Bean(name = "localDataSourceProperties")
//    @ConfigurationProperties(prefix = "app.datasource.local")
//    public DataSourceProperties localDataSourceProperties() {
//        return new DataSourceProperties();
//    }
//
//    @Primary
//    @Bean(name = "localDataSource")
//    @ConfigurationProperties(prefix = "app.datasource.local.hikari")
//    public DataSource localDataSource(
//            @Qualifier("localDataSourceProperties") DataSourceProperties localDataSourceProperties) {
//        return localDataSourceProperties.initializeDataSourceBuilder()
//                .type(HikariDataSource.class)
//                .build();
//    }
//
//    @Primary
//    @Bean(name = "localEntityManagerFactory")
//    public LocalContainerEntityManagerFactoryBean localEntityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("localDataSource") DataSource localDataSource) {
//
//        Map<String, Object> jpaProperties = new HashMap<>();
//        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
//        jpaProperties.put("hibernate.show_sql", false);
//        jpaProperties.put("hibernate.format_sql", false);
//        jpaProperties.put("hibernate.jdbc.time_zone", "Asia/Kolkata");
//
//        return builder
//                .dataSource(localDataSource)
//                .packages("org.backend.model")
//                .persistenceUnit("local")
//                .properties(jpaProperties)
//                .build();
//    }
//
//    @Primary
//    @Bean(name = "localTransactionManager")
//    public PlatformTransactionManager localTransactionManager(
//            @Qualifier("localEntityManagerFactory") LocalContainerEntityManagerFactoryBean localEntityManagerFactory) {
//        EntityManagerFactory emf = localEntityManagerFactory.getObject();
//        return new JpaTransactionManager(emf);
//    }
//}