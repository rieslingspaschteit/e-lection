package pse.election.backendserver.controller;

/**
 * This interface provides the e-lection functionalities of the api. The "outer world" can access
 * every functionality of the api via this interface. The
 * {@link pse.election.backendserver.controller.app.FrontController} implements this interface and
 * controls the api controller.
 *
 * @version 1.0
 */
public interface ElectionAPI extends ElectionAuthorityAPI, ElectionDataAPI, ElectionTrusteeAPI,
    ElectionUserAPI, ElectionVoterAPI {

}
