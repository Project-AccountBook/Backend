package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chaewookim.accountbookformoms.global.security.jwt.JwtTokenProvider;
import com.chaewookim.accountbookformoms.global.security.oauth2.CustomOAuth2UserService;
import com.chaewookim.accountbookformoms.global.security.oauth2.OAuth2SuccessHandler;
import com.chaewookim.accountbookformoms.domain.grouppruchase.application.*;
import com.chaewookim.accountbookformoms.global.security.config.SecurityConfig;
import com.chaewookim.accountbookformoms.global.error.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        GroupPurchaseController.class,
        AdminGroupPurchaseController.class,
        GroupPurchaseApplicationController.class,
        ProductController.class,
        GroupPurchaseCategoryController.class,
        ReportController.class,
        AdminReportController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockBean
    protected CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    protected OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    protected com.chaewookim.accountbookformoms.global.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @MockBean
    protected GroupPurchaseService groupPurchaseService;

    @MockBean
    protected GroupPurchaseLockFacade groupPurchaseLockFacade;

    @MockBean
    protected GroupPurchaseApplicationService groupPurchaseApplicationService;

    @MockBean
    protected ProductService productService;

    @MockBean
    protected GroupPurchaseCategoryService groupPurchaseCategoryService;

    @MockBean
    protected ReportService reportService;
}
