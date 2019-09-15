package com.mycorp;

import org.junit.Test;

import junit.framework.TestCase;
import util.datos.UsuarioAlta;

/**
 * Unit test for simple App.
 */
public class RealizarSimulacionTest extends TestCase {

    /**
     * Rigourous Test :-)
     */
    @Test
    public void testApp() {
    	UsuarioAlta usuarioAlta = new UsuarioAlta();
		String userAgent = "TEST";
		new ZendeskService().altaTicketZendesk(usuarioAlta, userAgent);
    }

}