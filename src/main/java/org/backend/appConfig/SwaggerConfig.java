package org.backend.appConfig;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Professional OpenAPI/Swagger Configuration for Stylo Customer Microservice.
 *
 * This configuration class provides enterprise-grade API documentation setup including:
 * - Comprehensive API information and contact details
 * - Multi-environment server configuration
 * - Advanced JWT Bearer authentication scheme
 * - Security requirements and global security definitions
 * - Custom schema components for improved documentation
 *
 * The Swagger UI will be accessible at: http://localhost:9091/swagger-ui.html
 * The OpenAPI JSON spec: http://localhost:9091/v3/api-docs
 *
 * @author Enterprise Architecture Team
 * @version 1.0.0
 * @since 2024
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:9091}")
    private String serverPort;

    /**
     * Configures and creates the customized OpenAPI specification bean.
     *
     * This method defines:
     * 1. API metadata (title, description, version, contact, license)
     * 2. Multi-environment server configurations with variables
     * 3. JWT Bearer Token authentication scheme
     * 4. Global security requirements
     * 5. Custom response schema components
     *
     * @return OpenAPI - Fully configured OpenAPI specification object
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                //.servers(buildServerConfigurations())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(buildSecurityComponents());
    }

    /**
     * Builds comprehensive API information section.
     * Includes title, description, version, contact details, and license information.
     *
     * @return Info - Configured API information object
     */
    private Info buildApiInfo() {
        return new Info()
                .title("Stylo Customer APP API")
                .description("Enterprise-grade RESTful API for Stylo customer management system.\n\n" +
                        "This Spring Boot project handles:\n" +
                        "• User authentication and authorization (OTP-based login)\n" +
                        "• Customer profile management and registration\n" +
                        "• Customer address management (CRUD operations)\n" +
                        "• Salon discovery and search capabilities\n" +
                        "• Salon service catalog management\n" +
                        "• Salon category organization\n" +
                        "• Salon resource management\n\n" +
                        "All endpoints require valid JWT authentication except public endpoints (send OTP, verify OTP).\n\n" +
                        "API Response Format: All responses follow a standardized wrapper format with status, message, and data fields.")
                .version("1.0.0");
        //.contact(buildContactInfo())
        //.license(buildLicenseInfo());
    }

    /**
     * Builds contact information for API support.
     * Provides contact details for API consumers and support channels.
     *
     * @return Contact - Configured contact information
     */
//    private Contact buildContactInfo() {
//        return new Contact()
//                .name("Stylo Development Team")
//                .url("https://stylo.com/support")
//                .email("api-support@stylo.com");
//    }

    /**
     * Builds license information for the API.
     *
     * @return License - Configured license information
     */
    private License buildLicenseInfo() {
        return new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");
    }

    /**
     * Builds multi-environment server configurations.
     * Allows easy switching between development, staging, and production environments.
     *
     * @return List<Server> - List of configured server environments
     */
    private List<Server> buildServerConfigurations() {
        return List.of(
                buildLocalServer(),
                buildDevelopmentServer(),
                buildStagingServer(),
                buildProductionServer()
        );
    }

    /**
     * Configures the local development server.
     *
     * @return Server - Local development server configuration
     */
    private Server buildLocalServer() {
        return new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server");
    }

    /**
     * Configures the development environment server.
     *
     * @return Server - Development environment configuration
     */
    private Server buildDevelopmentServer() {
        return new Server()
                .url("https://dev-api.stylo.com")
                .description("Development Environment");
    }

    /**
     * Configures the staging environment server.
     *
     * @return Server - Staging environment configuration
     */
    private Server buildStagingServer() {
        return new Server()
                .url("https://staging-api.stylo.com")
                .description("Staging Environment");
    }

    /**
     * Configures the production environment server.
     *
     * @return Server - Production environment configuration
     */
    private Server buildProductionServer() {
        return new Server()
                .url("https://api.stylo.com")
                .description("Production Environment");
    }

    /**
     * Builds security components including authentication schemes and schemas.
     * Configures JWT Bearer Token scheme and common response models.
     *
     * @return Components - Configured security components and schemas
     */
    private Components buildSecurityComponents() {
        Components components = new Components();

        // Configure JWT Bearer Token Security Scheme
        components.addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("JWT Bearer token for API authentication.\n\n" +
                                "Format: Bearer <token>\n\n" +
                                "Obtain token by:\n" +
                                "1. Call POST /auth/login/sendOTP with phone number\n" +
                                "2. Call POST /auth/login/verifyOTP with OTP code\n" +
                                "3. Use returned accessToken in Authorization header"));

        // Add custom schema components for better documentation
        addCommonSchemaComponents(components);

        return components;
    }

    /**
     * Adds common schema components used across multiple API endpoints.
     * These schemas improve documentation consistency and reduce duplication.
     *
     * @param components Components object to add schemas to
     */
    private void addCommonSchemaComponents(Components components) {
        // ApiResponse wrapper schema - used by all endpoints
        components.addSchemas("ApiResponse", createApiResponseSchema());

        // Error Response schema
        components.addSchemas("ErrorResponse", createErrorResponseSchema());

        // Pagination schema
        components.addSchemas("Pagination", createPaginationSchema());
    }

    /**
     * Creates the standard ApiResponse wrapper schema used across all endpoints.
     *
     * @return Schema - Configured ApiResponse schema
     */
    private Schema<?> createApiResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setDescription("Standard API Response wrapper for all endpoints");

        Map<String, Schema> properties = new HashMap<>();
        properties.put("status", createSchema("boolean", "Request status (true for success, false for failure)"));
        properties.put("message", createSchema("string", "Descriptive message about the operation"));
        properties.put("data", createSchema("object", "Response data payload (structure depends on endpoint)"));
        properties.put("timestamp", createSchema("string", "ISO 8601 formatted timestamp of the response"));

        schema.setProperties(properties);
        return schema;
    }

    /**
     * Creates the error response schema.
     *
     * @return Schema - Configured error response schema
     */
    private Schema<?> createErrorResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setDescription("Standard error response format");

        Map<String, Schema> properties = new HashMap<>();
        properties.put("status", createSchema("boolean", "Error status (always false)"));
        properties.put("message", createSchema("string", "Error message describing what went wrong"));
        properties.put("errorCode", createSchema("string", "Machine-readable error code for client handling"));
        properties.put("timestamp", createSchema("string", "ISO 8601 formatted timestamp of the error"));

        schema.setProperties(properties);
        return schema;
    }

    /**
     * Creates the pagination schema for paginated responses.
     *
     * @return Schema - Configured pagination schema
     */
    private Schema<?> createPaginationSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setDescription("Pagination metadata for list responses");

        Map<String, Schema> properties = new HashMap<>();
        properties.put("pageNumber", createSchema("integer", "Current page number (0-indexed)"));
        properties.put("pageSize", createSchema("integer", "Number of items per page"));
        properties.put("totalElements", createSchema("integer", "Total number of elements available"));
        properties.put("totalPages", createSchema("integer", "Total number of pages"));
        properties.put("hasNext", createSchema("boolean", "Whether there is a next page"));
        properties.put("hasPrevious", createSchema("boolean", "Whether there is a previous page"));

        schema.setProperties(properties);
        return schema;
    }

    /**
     * Helper method to create a simple schema property with type and description.
     *
     * @param type The data type of the property
     * @param description Description of the property
     * @return Schema - Configured schema property
     */
    private Schema<?> createSchema(String type, String description) {
        Schema<?> schema = new Schema<>();
        schema.setType(type);
        schema.setDescription(description);
        return schema;
    }
}
