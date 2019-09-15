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
		private IMocksControl control;
		private Map<String, Object> environment;
		private PortalClientesWebEJBRemote portalclientesWebEJBRemote;
		private RestTemplate restTemplate;
		private MensajeriaService emailService;

		public Configuracion() {
			this.zendeskService = new ZendeskService();

			IMocksControl control = EasyMock.createControl();
			this.control = control;

			Map<String, Object> environment = new HashMap<String, Object>();
			environment.put("zendesk.ticket", null);
			environment.put("zendesk.token", "token");
			environment.put("zendesk.url", "http://localhost:80/zendesk");
			environment.put("zendesk.user", "user");
			environment.put("tarjetas.getDatos", "http://localhost:80/tarjetas/getDatos/");
			environment.put("cliente.getDatos", "http://localhost:80/cliente/getDatos/");
			environment.put("zendesk.error.mail.funcionalidad", "42");
			environment.put("zendesk.error.destinatario", "destinatario@localhost");
			this.environment = environment;
			this.portalclientesWebEJBRemote = control.createMock(PortalClientesWebEJBRemote.class);
			this.restTemplate = control.createMock(RestTemplate.class);
			this.emailService = control.createMock(MensajeriaService.class);
		}

		@Bean
		public ZendeskService zendeskService() {
			return zendeskService;
		}

		@Bean
		public IMocksControl easymockcontrol() {
			return control;
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

	@Autowired
	private IMocksControl control;


	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MensajeriaService emailService;

	/**
	 * Test inicial de la app
	 */
	@Test
	public void testApp() {
		control.reset();

		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall();

		control.replay();

		// Llamada para dar de alta ticket en Zendesk
		zendeskService.altaTicketZendesk(new UsuarioAlta(), "TEST");

		control.verify();
	}

}