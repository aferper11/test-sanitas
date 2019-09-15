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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.mycorp.support.DatosCliente;
import com.mycorp.support.MensajeriaService;

import org.junit.Assert;
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
		private Zendesk zendesk;

		public Configuracion() {
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
			this.zendesk = control.createMock(Zendesk.class);
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

		@Bean
		public Zendesk zendesk() {
			return zendesk;
		}


	}

	@Autowired
	private ZendeskService zendeskService;

	@Autowired
	private IMocksControl control;


	@Autowired
	private PortalClientesWebEJBRemote portalclientesWebEJBRemote;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MensajeriaService emailService;

	@Autowired
	private Zendesk zendesk;

	@Test
	public void testVacio() {
		control.reset();

		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall();

		control.replay();

		// Llamada para dar de alta ticket en Zendesk
		String response = zendeskService.altaTicketZendesk(new UsuarioAlta(), "TEST");

		control.verify();

		Assert.assertEquals("Nº tarjeta Sanitas o Identificador: null\\nTipo documento: 0\\nNº documento: null\\nEmail personal: null\\nNº móvil: null\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\n", response);
	}

	@Test
	public void testTarjetaErrorClienteErrorTicketErrorEmailError() {
		control.reset();

		EasyMock.expect(restTemplate.getForEntity(EasyMock.eq("http://localhost:80/tarjetas/getDatos/123456"), EasyMock.eq(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall().andThrow(new UnsupportedOperationException("TESTING"));

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumTarjeta("123456");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº tarjeta Sanitas o Identificador: 123456\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\n", response);
	}

	@Test
	public void testTarjetaNotFoundClienteErrorTicketErrorEmailError() {
		control.reset();

		EasyMock.expect(restTemplate.getForEntity(EasyMock.eq("http://localhost:80/tarjetas/getDatos/123456"), EasyMock.eq(String.class))).andReturn(new ResponseEntity<String>(HttpStatus.NOT_FOUND));

		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall().andThrow(new UnsupportedOperationException("TESTING"));

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumTarjeta("123456");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº tarjeta Sanitas o Identificador: 123456\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\n", response);
	}

	@Test
	public void testTarjetaOkClienteErrorTicketErrorEmailError() {
		control.reset();

		EasyMock.expect(restTemplate.getForEntity(EasyMock.eq("http://localhost:80/tarjetas/getDatos/123456"), EasyMock.eq(String.class))).andReturn(new ResponseEntity<String>("USUARIO", HttpStatus.OK));

		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall().andThrow(new UnsupportedOperationException("TESTING"));

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumTarjeta("123456");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº tarjeta Sanitas o Identificador: 123456\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\n", response);
	}

	@Test
	public void testTarjetaOkClienteOkTicketErrorEmailError() {
		control.reset();

		EasyMock.expect(restTemplate.getForEntity(EasyMock.eq("http://localhost:80/tarjetas/getDatos/123456"), EasyMock.eq(String.class))).andReturn(new ResponseEntity<String>("USUARIO", HttpStatus.OK));

		DatosCliente cliente = new DatosCliente();
		cliente.setGenTGrupoTmk(Integer.valueOf(912345678));
		cliente.setFechaNacimiento("01/01/1990");
		cliente.setGenCTipoDocumento(Integer.valueOf(0));
		cliente.setNumeroDocAcred("87654321");
		cliente.setGenTTipoCliente(Integer.valueOf(1));
		cliente.setGenTStatus(Integer.valueOf(0));
		cliente.setIdMotivoAlta(Integer.valueOf(0));
		cliente.setfInactivoWeb("N");
		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andReturn(cliente);

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall().andThrow(new UnsupportedOperationException("TESTING"));

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumTarjeta("123456");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº tarjeta Sanitas o Identificador: 123456\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\nTeléfono: 912345678\\nFeha de nacimiento: 01/01/1990\\n", response);
	}

	@Test
	public void testTarjetaOkClienteOkTicketErrorEmailOk() {
		control.reset();

		EasyMock.expect(restTemplate.getForEntity(EasyMock.eq("http://localhost:80/tarjetas/getDatos/123456"), EasyMock.eq(String.class))).andReturn(new ResponseEntity<String>("USUARIO", HttpStatus.OK));

		DatosCliente cliente = new DatosCliente();
		cliente.setGenTGrupoTmk(Integer.valueOf(912345678));
		cliente.setFechaNacimiento("01/01/1990");
		cliente.setGenCTipoDocumento(Integer.valueOf(0));
		cliente.setNumeroDocAcred("87654321");
		cliente.setGenTTipoCliente(Integer.valueOf(1));
		cliente.setGenTStatus(Integer.valueOf(0));
		cliente.setIdMotivoAlta(Integer.valueOf(0));
		cliente.setfInactivoWeb("N");
		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andReturn(cliente);

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall();

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumTarjeta("123456");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº tarjeta Sanitas o Identificador: 123456\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\nTeléfono: 912345678\\nFeha de nacimiento: 01/01/1990\\n", response);
	}

	@Test
	public void testTarjetaOkClienteOkTicketOk() {
		control.reset();

		EasyMock.expect(restTemplate.getForEntity(EasyMock.eq("http://localhost:80/tarjetas/getDatos/123456"), EasyMock.eq(String.class))).andReturn(new ResponseEntity<String>("USUARIO", HttpStatus.OK));

		DatosCliente cliente = new DatosCliente();
		cliente.setGenTGrupoTmk(Integer.valueOf(912345678));
		cliente.setFechaNacimiento("01/01/1990");
		cliente.setGenCTipoDocumento(Integer.valueOf(0));
		cliente.setNumeroDocAcred("87654321");
		cliente.setGenTTipoCliente(Integer.valueOf(1));
		cliente.setGenTStatus(Integer.valueOf(0));
		cliente.setIdMotivoAlta(Integer.valueOf(0));
		cliente.setfInactivoWeb("N");
		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andReturn(cliente);

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andReturn(null);
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumTarjeta("123456");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº tarjeta Sanitas o Identificador: 123456\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\nTeléfono: 912345678\\nFeha de nacimiento: 01/01/1990\\n", response);
	}

	@Test
	public void testPolizaErrorClienteErrorTicketErrorEmailError() {
		control.reset();

		EasyMock.expect(portalclientesWebEJBRemote.recuperarDatosPoliza(EasyMock.anyObject(util.datos.PolizaBasico.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall().andThrow(new UnsupportedOperationException("TESTING"));

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumPoliza("12345");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº de poliza/colectivo: 12345/12345678\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\n", response);
	}

	@Test
	public void testPolizaOkClienteErrorTicketErrorEmailError() {
		control.reset();

		util.datos.DatosPersonales tomador = new util.datos.DatosPersonales();
		tomador.setNombre("NOMBRE-TOMADOR");
		tomador.setApellido1("APELLIDO1-TOMADOR");
		tomador.setApellido2("APELLIDO2-TOMADOR");
		tomador.setIdentificador("4444");
		util.datos.DetallePoliza detallePolizaResponse = new util.datos.DetallePoliza();
		detallePolizaResponse.setTomador(tomador);
		EasyMock.expect(portalclientesWebEJBRemote.recuperarDatosPoliza(EasyMock.anyObject(util.datos.PolizaBasico.class))).andReturn(detallePolizaResponse);

		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andThrow(new UnsupportedOperationException("TESTING"));

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall().andThrow(new UnsupportedOperationException("TESTING"));

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumPoliza("12345");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº de poliza/colectivo: 12345/12345678\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\n", response);
	}

	@Test
	public void testPolizaOkClienteOkTicketErrorEmailError() {
		control.reset();

		util.datos.DatosPersonales tomador = new util.datos.DatosPersonales();
		tomador.setNombre("NOMBRE-TOMADOR");
		tomador.setApellido1("APELLIDO1-TOMADOR");
		tomador.setApellido2("APELLIDO2-TOMADOR");
		tomador.setIdentificador("4444");
		util.datos.DetallePoliza detallePolizaResponse = new util.datos.DetallePoliza();
		detallePolizaResponse.setTomador(tomador);
		EasyMock.expect(portalclientesWebEJBRemote.recuperarDatosPoliza(EasyMock.anyObject(util.datos.PolizaBasico.class))).andReturn(detallePolizaResponse);

		DatosCliente cliente = new DatosCliente();
		cliente.setGenTGrupoTmk(Integer.valueOf(912345678));
		cliente.setFechaNacimiento("01/01/1990");
		cliente.setGenCTipoDocumento(Integer.valueOf(0));
		cliente.setNumeroDocAcred("87654321");
		cliente.setGenTTipoCliente(Integer.valueOf(1));
		cliente.setGenTStatus(Integer.valueOf(0));
		cliente.setIdMotivoAlta(Integer.valueOf(0));
		cliente.setfInactivoWeb("N");
		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andReturn(cliente);

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall().andThrow(new UnsupportedOperationException("TESTING"));

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumPoliza("12345");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº de poliza/colectivo: 12345/12345678\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\nTeléfono: 912345678\\nFeha de nacimiento: 01/01/1990\\n", response);
	}

	@Test
	public void testPolizaOkClienteOkTicketErrorEmailOk() {
		control.reset();

		util.datos.DatosPersonales tomador = new util.datos.DatosPersonales();
		tomador.setNombre("NOMBRE-TOMADOR");
		tomador.setApellido1("APELLIDO1-TOMADOR");
		tomador.setApellido2("APELLIDO2-TOMADOR");
		tomador.setIdentificador("4444");
		util.datos.DetallePoliza detallePolizaResponse = new util.datos.DetallePoliza();
		detallePolizaResponse.setTomador(tomador);
		EasyMock.expect(portalclientesWebEJBRemote.recuperarDatosPoliza(EasyMock.anyObject(util.datos.PolizaBasico.class))).andReturn(detallePolizaResponse);

		DatosCliente cliente = new DatosCliente();
		cliente.setGenTGrupoTmk(Integer.valueOf(912345678));
		cliente.setFechaNacimiento("01/01/1990");
		cliente.setGenCTipoDocumento(Integer.valueOf(0));
		cliente.setNumeroDocAcred("87654321");
		cliente.setGenTTipoCliente(Integer.valueOf(1));
		cliente.setGenTStatus(Integer.valueOf(0));
		cliente.setIdMotivoAlta(Integer.valueOf(0));
		cliente.setfInactivoWeb("N");
		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andReturn(cliente);

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andThrow(new UnsupportedOperationException("TESTING"));
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		emailService.enviar(EasyMock.anyObject(com.mycorp.support.CorreoElectronico.class));
		EasyMock.expectLastCall();

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumPoliza("12345");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº de poliza/colectivo: 12345/12345678\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\nTeléfono: 912345678\\nFeha de nacimiento: 01/01/1990\\n", response);
	}

	@Test
	public void testPolizaOkClienteOkTicketOk() {
		control.reset();

		util.datos.DatosPersonales tomador = new util.datos.DatosPersonales();
		tomador.setNombre("NOMBRE-TOMADOR");
		tomador.setApellido1("APELLIDO1-TOMADOR");
		tomador.setApellido2("APELLIDO2-TOMADOR");
		tomador.setIdentificador("4444");
		util.datos.DetallePoliza detallePolizaResponse = new util.datos.DetallePoliza();
		detallePolizaResponse.setTomador(tomador);
		EasyMock.expect(portalclientesWebEJBRemote.recuperarDatosPoliza(EasyMock.anyObject(util.datos.PolizaBasico.class))).andReturn(detallePolizaResponse);

		DatosCliente cliente = new DatosCliente();
		cliente.setGenTGrupoTmk(Integer.valueOf(912345678));
		cliente.setFechaNacimiento("01/01/1990");
		cliente.setGenCTipoDocumento(Integer.valueOf(0));
		cliente.setNumeroDocAcred("87654321");
		cliente.setGenTTipoCliente(Integer.valueOf(1));
		cliente.setGenTStatus(Integer.valueOf(0));
		cliente.setIdMotivoAlta(Integer.valueOf(0));
		cliente.setfInactivoWeb("N");
		EasyMock.expect(restTemplate.getForObject(EasyMock.eq("http://localhost:8080/test-endpoint"), EasyMock.eq(com.mycorp.support.DatosCliente.class), EasyMock.anyObject(String.class))).andReturn(cliente);

		EasyMock.expect(zendesk.createTicket(EasyMock.anyObject(com.mycorp.support.Ticket.class))).andReturn(null);
		zendesk.close();
		EasyMock.expectLastCall().anyTimes();

		control.replay();

		UsuarioAlta usuarioAlta = new UsuarioAlta();
		usuarioAlta.setTipoDocAcreditativo(1);
		usuarioAlta.setNumDocAcreditativo("12345678");
		usuarioAlta.setEmail("yo@localhost");
		usuarioAlta.setNumeroTelefono("123456879");
		usuarioAlta.setNumPoliza("12345");
		String userAgent = "TEST";

		String response = zendeskService.altaTicketZendesk(usuarioAlta, userAgent);

		control.verify();

		assertEquals("Nº de poliza/colectivo: 12345/12345678\\nTipo documento: 1\\nNº documento: 12345678\\nEmail personal: yo@localhost\\nNº móvil: 123456879\\nUser Agent: TEST\\n\\nDatos recuperados de BRAVO:\\n\\nTeléfono: 912345678\\nFeha de nacimiento: 01/01/1990\\n", response);
	}

}