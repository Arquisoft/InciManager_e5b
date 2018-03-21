package uo.asw.dbManagement.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import uo.asw.apacheKafka.producer.KafkaProducer;
import uo.asw.dbManagement.model.Agent;
import uo.asw.dbManagement.model.Incidence;
import uo.asw.dbManagement.model.Operator;
import uo.asw.dbManagement.repositories.AgentsRepository;
import uo.asw.dbManagement.repositories.IncidencesRepository;
import uo.asw.dbManagement.repositories.OperatorsRepository;
import uo.asw.reporter.InciReporter;

@Service
public class IncidenceService {

	@Autowired
	private AgentsRepository agentsRepository;

	@Autowired
	private IncidencesRepository incidenceRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private KafkaProducer kafkaProducer;

	private static final Logger logger = Logger.getLogger(KafkaProducer.class);

	public void sendCorrectIncidence(Incidence incidence) {
		kafkaProducer.send("incidences", incidence.getDescription());// Incidences es el topic para las
																		// incidencias(InicManager)
	}

	public boolean manageIncidence(String name, String password, String kind, Incidence incidence) {
		if (loginCorrecto(name, password, kind)) {
			persistIncidence(incidence);
			sendCorrectIncidence(incidence);
			return true;
		} else {
			reportIncidence(incidence);
			return false;
		}

	}

	private void persistIncidence(Incidence incidence) {
		incidenceRepository.save(incidence);
	}

	private boolean loginCorrecto(String name, String password, String kind) {
		logger.info("Sending POST request to url http://localhost:8080/user ");
		String url = "http://localhost:8080/user"; // Supuesta url desde donde se envían las peticiones
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_JSON);
		JSONObject peticion = new JSONObject();
		peticion.put("login", name);
		peticion.put("password", password);
		peticion.put("kind", kind);
		HttpEntity<String> entity = new HttpEntity<String>(peticion.toString(), header);
		ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.POST, entity, String.class);
		HttpStatus responseCode = response.getStatusCode();
		return responseCode.equals(HttpStatus.OK);
	}

	private void reportIncidence(Incidence incidence) {
		InciReporter.reportInci(incidence);
	}

}
