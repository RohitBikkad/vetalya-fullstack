package com.pet.vetalya.auth;

import com.pet.vetalya.auth.apple.AppleTokenRevocationService;
import com.pet.vetalya.auth.controller.AuthController;
import com.pet.vetalya.auth.service.*;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.dto.*;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.repository.*;
import com.pet.vetalya.common.GlobalExceptionHandler.BadRequestException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MultiClientAuthTest {
    AuthController controller;
    TokenService tokens;
    UserAuthRepository users;
    OneTimeCodeRepository codes;
    UserAuth user;
    @BeforeEach void setup() {
        tokens=mock(TokenService.class);users=mock(UserAuthRepository.class);codes=mock(OneTimeCodeRepository.class);
        var password=mock(PasswordEncoder.class);when(password.matches(anyString(),anyString())).thenReturn(true);
        controller=new AuthController(tokens,mock(SocialLoginService.class),mock(NativeTokenVerificationService.class),mock(AppleTokenRevocationService.class),codes,users,mock(AuthProviderRepository.class),mock(RefreshTokenRepository.class),
            new AppProperties("https://app.example.test","myvetapp",null,null,null,null,null,null,null,null),password,mock(PetOwnerRepository.class),mock(VetRepository.class),mock(OtpService.class));
        user=new UserAuth();user.setUserId("owner");user.setRole("ROLE_PET_OWNER");user.setEmail("owner@example.test");user.setPasswordHash("hash");
        when(users.findByEmail("owner@example.test")).thenReturn(Optional.of(user));when(users.findById("owner")).thenReturn(Optional.of(user));
        when(tokens.createAccessToken(user)).thenReturn("access");
        when(tokens.createRefreshToken(eq(user),anyString(),nullable(String.class),anyString())).thenReturn("refresh");
    }
    @Test void existingWebPasswordLoginUsesHttpOnlyCookieWithoutExposingRefreshInJson() {
        var request=new MockHttpServletRequest();request.setSecure(true);var response=new MockHttpServletResponse();
        var result=controller.login(Map.of("email","owner@example.test","password","sample"),request,response);
        assertEquals(200,result.getStatusCode().value());
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));assertTrue(response.getHeader("Set-Cookie").contains("Secure"));
        assertFalse(((Map<?,?>)result.getBody()).containsKey("refreshToken"));
        verify(tokens).createRefreshToken(eq(user),eq("web"),isNull(),anyString());
    }
    @Test void androidAndIosPasswordLoginReturnTokensWithoutSettingCookies() {
        for(String platform:List.of("android","ios")) {
            var response=new MockHttpServletResponse();
            var result=controller.login(Map.of("email","owner@example.test","password","sample","clientType",platform,"deviceId","phone-1"),new MockHttpServletRequest(),response);
            assertEquals(200,result.getStatusCode().value());assertNull(response.getHeader("Set-Cookie"));
            assertEquals("refresh",((Map<?,?>)result.getBody()).get("refreshToken"));
            verify(tokens).createRefreshToken(eq(user),eq(platform),eq("phone-1"),anyString());
        }
    }
    @Test void unknownClientIsRejectedBeforeTokenIssuance() {
        assertThrows(BadRequestException.class,()->controller.login(Map.of("email","owner@example.test","password","sample","clientType","admin"),new MockHttpServletRequest(),new MockHttpServletResponse()));
        verifyNoInteractions(tokens);
    }
    @Test void nativeBodyRefreshWinsOverAnUnrelatedBrowserCookie() {
        when(tokens.rotateRefreshToken(eq("native-token"),anyString())).thenReturn(new TokenService.RotationResult("next-access","next-refresh",user));
        var request=new MockHttpServletRequest();request.setCookies(new Cookie("refresh_token","unrelated-cookie"));var response=new MockHttpServletResponse();
        var result=controller.refresh(new RefreshRequest("native-token","phone-1"),request,response);
        assertEquals("next-refresh",((TokenResponse)result.getBody()).refreshToken());assertNull(response.getHeader("Set-Cookie"));
    }
    @Test void socialRegistrationCannotCreateAnAdministratorAccount() {
        var service=new SocialLoginService(users,mock(AuthProviderRepository.class),mock(VetRepository.class),mock(PetOwnerRepository.class),tokens);
        var identity=new ProviderUserInfo("GOOGLE","new-provider-subject",null,false,"New","User",null);
        assertThrows(IllegalArgumentException.class,()->service.processLogin(identity,"ROLE_PLATFORM_ADMIN","android","phone-1","127.0.0.1"));
        verify(users,never()).save(any());verifyNoInteractions(tokens);
    }
    @Test void mobileCodeExchangeUsesLockedCodeAndNeverSetsBrowserCookie() {
        var code=new OneTimeCode();code.setUserId("owner");code.setClientType("ios");code.setExpiresAt(Instant.now().plusSeconds(60));
        when(codes.findByCodeHashForUpdate(anyString())).thenReturn(Optional.of(code));
        var response=new MockHttpServletResponse();
        var result=controller.exchange(new CodeExchangeRequest("sample-code","phone-1"),new MockHttpServletRequest(),response);
        assertEquals(200,result.getStatusCode().value());assertTrue(code.isUsed());assertNull(response.getHeader("Set-Cookie"));
        assertEquals("refresh",((TokenResponse)result.getBody()).refreshToken());
        verify(codes).findByCodeHashForUpdate(anyString());
        var replay=controller.exchange(new CodeExchangeRequest("sample-code","phone-1"),new MockHttpServletRequest(),new MockHttpServletResponse());
        assertEquals(401,replay.getStatusCode().value());
    }
    @Test void petOwnerRegistrationWorksWithDemoLoginDisabledForWebAndMobile() {
        var social=(SocialLoginService)org.springframework.test.util.ReflectionTestUtils.getField(controller,"socialLoginService");
        var password=(PasswordEncoder)org.springframework.test.util.ReflectionTestUtils.getField(controller,"passwordEncoder");
        var otp=(OtpService)org.springframework.test.util.ReflectionTestUtils.getField(controller,"otpService");
        when(otp.consumeEmailVerification(eq("new-owner@example.test"),eq("verified-email-proof"))).thenReturn(true);
        when(password.encode("StagingTest!234")).thenReturn("encoded-password");
        when(social.processLogin(any(ProviderUserInfo.class),eq("ROLE_PET_OWNER"),anyString(),nullable(String.class),anyString()))
            .thenReturn(new SocialLoginService.LoginResult(user,"access","refresh",true,true));
        for(String platform:List.of("web","android","ios")) {
            var request=new MockHttpServletRequest();request.setSecure(true);
            var response=new MockHttpServletResponse();
            var result=controller.register(Map.of("fullName","Test Owner","email","new-owner@example.test","phone","+12025550199","password","StagingTest!234","clientType",platform,"emailVerificationToken","verified-email-proof"),request,response);
            assertEquals(201,result.getStatusCode().value());
            assertEquals("encoded-password",user.getPasswordHash());
            var body=(Map<?,?>)result.getBody();
            assertEquals("access",body.get("accessToken"));
            if(platform.equals("web")) {
                assertFalse(body.containsKey("refreshToken"));
                assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));
            } else {
                assertEquals("refresh",body.get("refreshToken"));
                assertNull(response.getHeader("Set-Cookie"));
            }
        }
        verify(users,times(3)).save(user);
    }
    @Test void registrationValidatesRequiredPhoneWithDemoLoginDisabled() {
        var result=controller.register(Map.of("fullName","Test Owner","email","new-owner@example.test","password","StagingTest!234"),new MockHttpServletRequest(),new MockHttpServletResponse());
        assertEquals(400,result.getStatusCode().value());
        assertTrue(result.getBody().toString().contains("Mobile phone number is required"));
    }

    @Test void registrationWithoutEmailProofCannotCreateAnAccount() {
        var result=controller.register(Map.of("fullName","Test Owner","email","new-owner@example.test","phone","+12025550199","password","StagingTest!234"),new MockHttpServletRequest(),new MockHttpServletResponse());
        assertEquals(400,result.getStatusCode().value());
        assertTrue(result.getBody().toString().contains("Verify your email"));
        verify(users,never()).save(any());
    }
    @Test void otpResponsesNeverExposeCodesAndDeliveryFailureIsNotSuccess() {
        var otp=(OtpService)org.springframework.test.util.ReflectionTestUtils.getField(controller,"otpService");
        when(otp.sendOtp("owner@example.test","REGISTRATION")).thenReturn(new OtpService.OtpResult(true,"sent","123456",600));
        var result=controller.sendOtp(Map.of("email","owner@example.test","purpose","REGISTRATION"));
        assertEquals(200,result.getStatusCode().value());
        assertFalse(result.getBody().toString().contains("123456"));
        assertFalse(((Map<?,?>)result.getBody()).containsKey("devOtp"));
        when(otp.sendOtp("owner@example.test","REGISTRATION")).thenThrow(new IllegalStateException("sensitive provider details"));
        result=controller.sendOtp(Map.of("email","owner@example.test","purpose","REGISTRATION"));
        assertEquals(503,result.getStatusCode().value());assertFalse(result.getBody().toString().contains("sensitive"));
    }

}
