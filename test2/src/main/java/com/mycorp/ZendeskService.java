package com.mycorp;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycorp.support.CorreoElectronico;
import com.mycorp.support.DatosCliente;
import com.mycorp.support.MensajeriaService;
import com.mycorp.support.Poliza;
import com.mycorp.support.PolizaBasicoFromPolizaBuilder;
import com.mycorp.support.Ticket;
import com.mycorp.support.ValueCode;

import portalclientesweb.ejb.interfaces.PortalClientesWebEJBRemote;
import util.datos.PolizaBasico;
import util.datos.UsuarioAlta;

@Service
public class ZendeskService {

	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(ZendeskService.class);

	private static final String ESCAPED_LINE_SEPARATOR = "\\n";
	private static final String ESCAPE_ER = "\\";
	private static final String HTML_BR = "<br/>";
	@Value("#{envPC['zendesk.ticket']}")
	public String PETICION_ZENDESK = "";

	@Value("#{envPC['zendesk.token']}")
	public String TOKEN_ZENDESK = "";

	@Value("#{envPC['zendesk.url']}")
	public String URL_ZENDESK = "";

	@Value("#{envPC['zendesk.user']}")
	public String ZENDESK_USER = "";

	@Value("#{envPC['tarjetas.getDatos']}")
	public String TARJETAS_GETDATOS = "";

	@Value("#{envPC['cliente.getDatos']}")
	public String CLIENTE_GETDATOS = "";

	@Value("#{envPC['zendesk.error.mail.funcionalidad']}")
	public String ZENDESK_ERROR_MAIL_FUNCIONALIDAD = "";

	@Value("#{envPC['zendesk.error.destinatario']}")
	public String ZENDESK_ERROR_DESTINATARIO = "";

	/** The portalclientes web ejb remote. */
	@Autowired
	// @Qualifier("portalclientesWebEJB")
	private PortalClientesWebEJBRemote portalclientesWebEJBRemote;

	/** The rest template. */
	@Autowired
	@Qualifier("restTemplateUTF8")
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier("emailService")
	MensajeriaService emailService;

	private static final Map<Integer, String> TIPOS_CLIENTE;
	static {
		Map<Integer, String> tiposCliente = new HashMap<Integer, String>();
		tiposCliente.put(Integer.valueOf(1), "POTENCIAL");
		tiposCliente.put(Integer.valueOf(2), "REAL");
		tiposCliente.put(Integer.valueOf(3), "PROSPECTO");
		TIPOS_CLIENTE = tiposCliente;
	}

	/**
	 * Crea un ticket en Zendesk. Si se ha informado el nº de tarjeta, obtiene los
	 * datos asociados a dicha tarjeta de un servicio externo.
	 * 
	 * @param usuarioAlta
	 *            {@link UsuarioAlta} obj del usuario que se va a dar de alta
	 * @param userAgent
	 *            {@link String} tipo del usuario
	 */
	public String altaTicketZendesk(UsuarioAlta usuarioAlta, String userAgent) {

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		String idCliente = null;

		StringBuilder clientName = new StringBuilder();

		// Añade los datos del formulario
		String datosUsuario = getDatosUsuario(usuarioAlta, userAgent);

		StringBuilder datosServicio = new StringBuilder();
		// Obtiene el idCliente de la tarjeta
		if (StringUtils.isNotBlank(usuarioAlta.getNumTarjeta())) {
			try {
				String urlToRead = TARJETAS_GETDATOS + usuarioAlta.getNumTarjeta();
				ResponseEntity<String> res = restTemplate.getForEntity(urlToRead, String.class);
				if (res.getStatusCode() == HttpStatus.OK) {
					String dusuario = res.getBody();
					clientName.append(dusuario);
					idCliente = dusuario;
					datosServicio.append("Datos recuperados del servicio de tarjeta:").append(ESCAPED_LINE_SEPARATOR)
							.append(mapper.writeValueAsString(dusuario));
				}
			} catch (Exception e) {
				LOG.error("Error al obtener los datos de la tarjeta", e);
			}
		} else if (StringUtils.isNotBlank(usuarioAlta.getNumPoliza())) {
			try {
				Poliza poliza = new Poliza();
				poliza.setNumPoliza(Integer.valueOf(usuarioAlta.getNumPoliza()));
				poliza.setNumColectivo(Integer.valueOf(usuarioAlta.getNumDocAcreditativo()));
				poliza.setCompania(1);

				PolizaBasico polizaBasicoConsulta = new PolizaBasicoFromPolizaBuilder().withPoliza(poliza).build();

				final util.datos.DetallePoliza detallePolizaResponse = portalclientesWebEJBRemote
						.recuperarDatosPoliza(polizaBasicoConsulta);

				clientName.append(detallePolizaResponse.getTomador().getNombre()).append(" ")
						.append(detallePolizaResponse.getTomador().getApellido1()).append(" ")
						.append(detallePolizaResponse.getTomador().getApellido2());

				idCliente = detallePolizaResponse.getTomador().getIdentificador();
				datosServicio.append("Datos recuperados del servicio de tarjeta:").append(ESCAPED_LINE_SEPARATOR)
						.append(mapper.writeValueAsString(detallePolizaResponse));
			} catch (Exception e) {
				LOG.error("Error al obtener los datos de la poliza", e);
			}
		}

		String datosBravo = getDatosBravo(idCliente);

		String ticket = String.format(PETICION_ZENDESK, clientName.toString(), usuarioAlta.getEmail(),
				datosUsuario + datosBravo + parseJsonBravo(datosServicio));
		ticket = ticket.replaceAll("[" + ESCAPED_LINE_SEPARATOR + "]", " ");

		try (Zendesk zendesk = newZendesk()) {
			// Ticket
			Ticket petiZendesk = mapper.readValue(ticket, Ticket.class);
			zendesk.createTicket(petiZendesk);

		} catch (Exception e) {
			LOG.error("Error al crear ticket ZENDESK", e);
			// Send email

			CorreoElectronico correo = new CorreoElectronico(Long.parseLong(ZENDESK_ERROR_MAIL_FUNCIONALIDAD), "es")
					.addParam(datosUsuario.replaceAll(ESCAPE_ER + ESCAPED_LINE_SEPARATOR, HTML_BR))
					.addParam(datosBravo.replaceAll(ESCAPE_ER + ESCAPED_LINE_SEPARATOR, HTML_BR));
			correo.setEmailA(ZENDESK_ERROR_DESTINATARIO);
			try {
				emailService.enviar(correo);
			} catch (Exception ex) {
				LOG.error("Error al enviar mail", ex);
			}

		}

		return datosUsuario + datosBravo;
	}

	protected String getDatosUsuario(UsuarioAlta usuarioAlta, String userAgent) {
		StringBuilder /**
						 * Método para obtener los datos del usuario
						 * 
						 * @param usuarioAlta
						 *            {@link UsuarioAlta} obj usuario
						 * @param userAgent
						 *            {@link String} tipo usuario
						 * 
						 * @return {@link String} datos del usuario
						 */
		datosUsuario = new StringBuilder();
		if (StringUtils.isNotBlank(usuarioAlta.getNumPoliza())) {
			datosUsuario.append("Nº de poliza/colectivo: ").append(usuarioAlta.getNumPoliza()).append("/")
					.append(usuarioAlta.getNumDocAcreditativo()).append(ESCAPED_LINE_SEPARATOR);
		} else {
			datosUsuario.append("Nº tarjeta Sanitas o Identificador: ").append(usuarioAlta.getNumTarjeta())
					.append(ESCAPED_LINE_SEPARATOR);
		}
		datosUsuario.append("Tipo documento: ").append(usuarioAlta.getTipoDocAcreditativo())
				.append(ESCAPED_LINE_SEPARATOR);
		datosUsuario.append("Nº documento: ").append(usuarioAlta.getNumDocAcreditativo())
				.append(ESCAPED_LINE_SEPARATOR);
		datosUsuario.append("Email personal: ").append(usuarioAlta.getEmail()).append(ESCAPED_LINE_SEPARATOR);
		datosUsuario.append("Nº móvil: ").append(usuarioAlta.getNumeroTelefono()).append(ESCAPED_LINE_SEPARATOR);
		datosUsuario.append("User Agent: ").append(userAgent).append(ESCAPED_LINE_SEPARATOR);
		return datosUsuario.toString();
	}

	/**
	 * Método para obtener los datos del cliente en BRAVO
	 * 
	 * @param idCliente
	 *            {@link String} id del cliente
	 * 
	 * @return {@link String} datos BRAVO del cliente
	 */
	protected String getDatosBravo(String idCliente) {
		StringBuilder datosBravo = new StringBuilder();
		datosBravo.append(ESCAPED_LINE_SEPARATOR + "Datos recuperados de BRAVO:" + ESCAPED_LINE_SEPARATOR
				+ ESCAPED_LINE_SEPARATOR);
		try {
			// Obtenemos los datos del cliente
			DatosCliente cliente = restTemplate.getForObject("http://localhost:8080/test-endpoint", DatosCliente.class,
					idCliente);

			datosBravo.append("Teléfono: ").append(cliente.getGenTGrupoTmk()).append(ESCAPED_LINE_SEPARATOR);

			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			String fechaNacimiento = cliente.getFechaNacimiento();
			fechaNacimiento = formatter.format(formatter.parse(fechaNacimiento)); // TODO considerar si esta linea sobra
			datosBravo.append("Feha de nacimiento: ").append(fechaNacimiento).append(ESCAPED_LINE_SEPARATOR);

			List<ValueCode> tiposDocumentos = getTiposDocumentosRegistro();
			for (int i = 0; i < tiposDocumentos.size(); i++) {
				if (tiposDocumentos.get(i).getCode().equals(cliente.getGenCTipoDocumento().toString())) {
					datosBravo.append("Tipo de documento: ").append(tiposDocumentos.get(i).getValue())
							.append(ESCAPED_LINE_SEPARATOR);
				}
			}
			datosBravo.append("Número documento: ").append(cliente.getNumeroDocAcred()).append(ESCAPED_LINE_SEPARATOR);

			datosBravo.append("Tipo cliente: ");
			if (TIPOS_CLIENTE.containsKey(cliente.getGenTTipoCliente())) {
				datosBravo.append(TIPOS_CLIENTE.get(cliente.getGenTTipoCliente())).append(ESCAPED_LINE_SEPARATOR);
			}

			datosBravo.append("ID estado del cliente: ").append(cliente.getGenTStatus()).append(ESCAPED_LINE_SEPARATOR);

			datosBravo.append("ID motivo de alta cliente: ").append(cliente.getIdMotivoAlta())
					.append(ESCAPED_LINE_SEPARATOR);

			datosBravo.append("Registrado: ").append((cliente.getfInactivoWeb() == null ? "Sí" : "No"))
					.append(ESCAPED_LINE_SEPARATOR + ESCAPED_LINE_SEPARATOR);

		} catch (Exception e) {
			LOG.error("Error al obtener los datos en BRAVO del cliente", e);
		}
		return datosBravo.toString();
	}

	/**
	 * Método factoría para encapsular la creación del Zendesk
	 * 
	 * @return Zendesk configurado
	 */
	protected Zendesk newZendesk() {
		return new Zendesk.Builder(URL_ZENDESK).setUsername(ZENDESK_USER).setToken(TOKEN_ZENDESK).build();
	}

	/**
	 * Método para obtener los tipo de documentos de registro
	 * 
	 * @return {@link List}
	 */
	public List<ValueCode> getTiposDocumentosRegistro() {
		return Arrays.asList(new ValueCode(), new ValueCode()); // simulacion servicio externo
	}

	/**
	 * Método para parsear el JSON de respuesta de los servicios de tarjeta/póliza
	 *
	 * @param resBravo
	 *            {@link StringBuilder} respuesta del servicio REST en JSON
	 * 
	 * @return {@link String} respuesta del servicio REST parseada a String
	 */
	private String parseJsonBravo(StringBuilder resBravo) {
		return resBravo.toString().replaceAll("[\\[\\]\\{\\}\\\"\\r]", "").replaceAll(ESCAPED_LINE_SEPARATOR,
				ESCAPE_ER + ESCAPED_LINE_SEPARATOR);
	}
}