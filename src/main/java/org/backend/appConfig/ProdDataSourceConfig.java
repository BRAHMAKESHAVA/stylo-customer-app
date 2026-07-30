//package org.backend.appConfig;
//
//import org.backend.model.PartnerDetails;
//import org.backend.model.SalonDetails;
//import org.backend.model.SalonImages;
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
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import javax.sql.DataSource;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * PROD datasource (Postgres).
// * Only the entities that actually exist in prod are wired here:
// * SalonDetails, SalonImages, PartnerDetails.
// * (Users comes from LOCAL only — see LocalDataSourceConfig.)
// *
// * hibernate.hbm2ddl.auto is set to "none" on purpose: this app never
// * creates, alters, or even validates the schema against prod. It only
// * connects and reads/writes through the mapped entities as-is. (Strict
// * "validate" mode was rejecting minor entity/schema mismatches — e.g.
// * nullable flags — that don't matter for read-only testing.)
// *
// * Everything else (Booking, Address, Coupon, Package, Payment, Category,
// * SalonResource, SalonService, NotificationDevice, etc.) is served by
// * LocalDataSourceConfig instead — see that class.
// */
//@Configuration
//@EnableJpaRepositories(
//        basePackages = "org.backend.repository",
//        entityManagerFactoryRef = "prodEntityManagerFactory",
//        transactionManagerRef = "prodTransactionManager",
//        includeFilters = @ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {
//                        SalonRepository.class,
//                        SalonImagesRepository.class,
//                        PartnerRepository.class
//                }
//        )
//)
//public class ProdDataSourceConfig {
//
//    @Bean(name = "prodDataSourceProperties")
//    @ConfigurationProperties(prefix = "app.datasource.prod")
//    public DataSourceProperties prodDataSourceProperties() {
//        return new DataSourceProperties();
//    }
//
//    @Bean(name = "prodDataSource")
//    @ConfigurationProperties(prefix = "app.datasource.prod.hikari")
//    public DataSource prodDataSource(
//            @Qualifier("prodDataSourceProperties") DataSourceProperties prodDataSourceProperties) {
//        return prodDataSourceProperties.initializeDataSourceBuilder()
//                .type(HikariDataSource.class)
//                .build();
//    }
//
//    @Bean(name = "prodEntityManagerFactory")
//    public LocalContainerEntityManagerFactoryBean prodEntityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("prodDataSource") DataSource prodDataSource) {
//
//        Map<String, Object> jpaProperties = new HashMap<>();
//        // "none": never touches prod schema, and also skips Hibernate's strict
//        // column-by-column validation (nullable flags, types, etc.) against the
//        // live prod schema, which was blocking startup on mismatches that don't
//        // matter for read-only testing. Use "validate" instead once the entity
//        // classes are kept in exact sync with the prod schema.
//        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
//        jpaProperties.put("hibernate.show_sql", false);
//        jpaProperties.put("hibernate.format_sql", false);
//        jpaProperties.put("hibernate.jdbc.time_zone", "Asia/Kolkata");
//
//        return builder
//                .dataSource(prodDataSource)
//                .managedTypes(PersistenceManagedTypes.of(
//                        SalonDetails.class.getName(),
//                        SalonImages.class.getName(),
//                        PartnerDetails.class.getName()
//                ))
//                .persistenceUnit("prod")
//                .properties(jpaProperties)
//                .build();
//    }
//
//    @Bean(name = "prodTransactionManager")
//    public PlatformTransactionManager prodTransactionManager(
//            @Qualifier("prodEntityManagerFactory") LocalContainerEntityManagerFactoryBean prodEntityManagerFactory) {
//        EntityManagerFactory emf = prodEntityManagerFactory.getObject();
//        return new JpaTransactionManager(emf);
//    }
//}