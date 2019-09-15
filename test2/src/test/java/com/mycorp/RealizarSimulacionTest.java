package com.mycorp;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.mycorp.support.MensajeriaService;

import junit.framework.TestCase;
import portalclientesweb.ejb.interfaces.PortalClientesWebEJBRemote;
import util.datos.UsuarioAlta;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RealizarSimulacionTest extends TestCase {

	@Configuration
	static class Configuracion {

		private ZendeskService zendeskService;
		private Map<String, Object> environment;
		private PortalClientesWebEJBRemote portalclientesWebEJBRemote;
		private RestTemplate restTemplate;
		private MensajeriaService emailService;

		public Configuracion() {
			this.zendeskService = new ZendeskService();

			IMocksControl control = EasyMock.createControl();

			Map<String, Object> environment = new HashMap<String, Object>();
			environment.put("zendesk.ticket", "");
			environment.put("zendesk.token", "");
			environment.put("zendesk.url", "");
			environment.put("zendesk.user", "");
			environment.put("tarjetas.getDatos", "");
			environment.put("cliente.getDatos", "");
			environment.put("zendesk.error.mail.funcionalidad", "");
			environment.put("zendesk.error.destinatario", "");
			this.environment = environment;
			this.portalclientesWebEJBRemote = control.createMock(PortalClientesWebEJBRemote.class);
			this.restTemplate = control.createMock(RestTemplate.class);
			this.emailService = control.createMock(MensajeriaService.class);
		}

		@Bean
		public ZendeskService zendeskService() {
			return zendeskService;
		}

		@Bean(name = "envPC")
		public Map<String, Object> environment() {
			return environment;
		}

		@Bean
		public PortalClientesWebEJBRemote portalclientesWebEJBRemote() {
			return portalclientesWebEJBRemote;
		}

		@Bean(name="restTemplateUTF8")
		public RestTemplate restTemplate() {
			return restTemplate;
		}

		@Bean
		public MensajeriaService emailService() {
			return emailService;
		}

	}

	@Autowired
	private ZendeskService zendeskService;

    @Test
    public void testApp() {
		zendeskService.altaTicketZendesk(new UsuarioAlta(), "TEST");
    }

}