package org.example.auth;

import com.google.gson.Gson;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.example.backend.UserSession;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.vaadin.viritin.layouts.MVerticalLayout;

/**
 *
 * @author Matti Tahvonen
 */
public class LoginWindow extends Window implements RequestHandler {

    private Link gplusLoginButton;
    private Link faceBookButton;

    OAuthService googleService;
    OAuthService fbService;

    @Inject
    UserSession userSession;
    final String redirectUrl;

    @Override
    public void attach() {
        super.attach();

        googleService = createGoogleService();
        String googleUrl = googleService.getAuthorizationUrl(null);
        fbService  = createFBService();
        String fbUrl = fbService.getAuthorizationUrl(null);
        
        gplusLoginButton = new Link("Login with Google", new ExternalResource(googleUrl));
        gplusLoginButton.addStyleName(ValoTheme.LINK_LARGE);
        
        faceBookButton = new Link("Login with Facebook", new ExternalResource(fbUrl));
        faceBookButton.addStyleName(ValoTheme.LINK_LARGE);

        VaadinSession.getCurrent().addRequestHandler(this);

        MVerticalLayout mVerticalLayout = new MVerticalLayout(gplusLoginButton, faceBookButton);
        
		setContent(mVerticalLayout.alignAll(
                Alignment.MIDDLE_CENTER).withFullHeight());
        setModal(true);
        setWidth("300px");
        setHeight("200px");

    }

    public void authDenied(String reason) {
        Notification.show("authDenied:" + reason,
                Notification.Type.ERROR_MESSAGE);
    }

    private OAuthService createGoogleService() {
        ServiceBuilder sb = new ServiceBuilder();
        sb.provider(Google2Api.class);
        sb.apiKey(gpluskey);
        sb.apiSecret(gplussecret);
        sb.scope("email");
        String callBackUrl = Page.getCurrent().getLocation().toString();
        if(callBackUrl.contains("#")) {
            callBackUrl = callBackUrl.substring(0, callBackUrl.indexOf("#"));
        }
        sb.callback(callBackUrl);
        return sb.build();
    }
    
    private OAuthService createFBService() {
    	ServiceBuilder sb = new ServiceBuilder();
    	sb.provider(FacebookApi.class);
    	sb.apiKey("455366335002918");
        sb.apiSecret("4dd7c3c902bd3e158e15353f59cd4df8");
        sb.scope("email");
        String callBackUrl = Page.getCurrent().getLocation().toString();
        if(callBackUrl.contains("#")) {
            callBackUrl = callBackUrl.substring(0, callBackUrl.indexOf("#"));
        }
        sb.callback(callBackUrl);
    	return sb.build();
    }

    public LoginWindow() {
        super("Login");
        redirectUrl = Page.getCurrent().getLocation().toString();

    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (request.getParameter("code") != null) {
            String code = request.getParameter("code");
            Verifier v = new Verifier(code);
            Token t = googleService.getAccessToken(null, v);

            OAuthRequest r = new OAuthRequest(Verb.GET,
                    "https://www.googleapis.com/plus/v1/people/me");
            googleService.signRequest(t, r);
            Response resp = r.send();
            System.out.println("Json response="+resp.getBody());
            GooglePlusAnswer answer = new Gson().fromJson(resp.getBody(),
                    GooglePlusAnswer.class);

            //
            String displayName  = answer.displayName;
            String emailArr = Arrays.toString(answer.emails);
            System.out.println("Display Name="+displayName+", emailArr="+emailArr);
            
            if(answer.emails.length==0)
            	return false;
            userSession.login(answer.emails[0].value, answer.displayName);

            close();
            VaadinSession.getCurrent().removeRequestHandler(this);

            ((VaadinServletResponse) response).getHttpServletResponse().
                    sendRedirect(redirectUrl);
            return true;
        }

        return false;
    }

    
    
    private String gpluskey="705099353658-rghjmk39n95am9j7bnjg4283eb7dcvgj.apps.googleusercontent.com";
    private String gplussecret="y-EmN_1yjLLYCs5Dn8TD3gAj";
    //https://console.developers.google.com/apis/credentials/oauthclient/705099353658-rghjmk39n95am9j7bnjg4283eb7dcvgj.apps.googleusercontent.com?project=tranquil-buffer-235404&angularJsUrl=%2Fapis%2Fcredentials%2Foauthclient%2F705099353658-rghjmk39n95am9j7bnjg4283eb7dcvgj.apps.googleusercontent.com%3Fproject%3D705099353658&authuser=2
    //AIzaSyABkEv8xBQov75kXQVih6TmvWFGHNPbIO0

}
