package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS.Nodo;

public class AgenteDFS extends AbstractPlayer {

	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//ArrayList con el plan a seguir
	private Stack<Types.ACTIONS> plan = new Stack<Types.ACTIONS>();
	
	//ArrayList con los muros y pinchos en el mapa
	private ArrayList<Nodo> muros_y_pinchos = new ArrayList<>();
	
	//Contador de las llamadas al método act
	int num_llamadas=0;
	
	//Nodo inicial y final
	Nodo avatar,portal;
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteDFS(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
		//Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length , 
        		stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);      
      
        //Se crea una lista de observaciones de portales, ordenada por cercania al avatar
        ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
        //Seleccionamos coordenadas del Portal
        portal_coordenadas = posiciones[0].get(0).position;
        portal_coordenadas.x = Math.floor(portal_coordenadas.x / fescala.x);
        portal_coordenadas.y = Math.floor(portal_coordenadas.y / fescala.y);
        
        // Definimos el nodo objetivo
        portal = new Nodo(portal_coordenadas);

        //Obtenemos las posiciones de los muros y pinchos
        ArrayList<Observation>[] obstaculos = stateObs.getImmovablePositions();
        for (int i = 0; i < obstaculos[0].size(); i++){
            //Obtenemos la posición de cada uno
            muros_y_pinchos.add( new Nodo(new Vector2d(Math.floor(obstaculos[0].get(i).position.x / fescala.x), Math.floor(obstaculos[0].get(i).position.y / fescala.y))));
        }
        
        for (int i = 0; i < obstaculos[1].size(); i++){
            //Obtenemos la posición de cada uno
            muros_y_pinchos.add( new Nodo(new Vector2d(Math.floor(obstaculos[1].get(i).position.x / fescala.x), Math.floor(obstaculos[1].get(i).position.y / fescala.y))));
        }
        
      //Posicion del avatar en coordenadas
        Vector2d pos_avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x, 
        		stateObs.getAvatarPosition().y / fescala.y);
      //Pareja, posición/estado del avatar
        avatar=new Nodo(pos_avatar);
	}

	/**
	 * return the best action to arrive faster to the closest portal
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return best	ACTION to arrive faster to the closest portal
	 */
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        if(num_llamadas==0) {
        	//Llamamos al plan con la información del lugar dónde se encuentran los muros
        	num_llamadas++;
        	DFS(avatar,portal,muros_y_pinchos,stateObs);
    		return plan.pop();
        }else {
    		return plan.pop();
        }
        
		
	}
	
	
	public void DFS (Nodo inicial, Nodo objetivo,ArrayList<Nodo> muros,StateObservation stateObs) {
		Hashtable<Double,Boolean> estado= new Hashtable<Double,Boolean>();
		//Marcamos el nodo inicial como visitado
		estado.put(inicial.id, true);
		System.out.println("Voy a llamar a DFS_search");
		DFS_search(inicial,objetivo,muros,stateObs,estado);
	}
	
	public Boolean DFS_search(Nodo u, Nodo objetivo,ArrayList<Nodo> muros,StateObservation stateObs,Hashtable<Double,Boolean> estado){
		ArrayList<Nodo> sucesores= new ArrayList<>();
		if(u.equals(objetivo)){
			System.out.println("calculamos el plan");
			plan=u.calculaCamino();
			System.out.println(plan.size());
			System.out.println("Hemos acabado de calcular el plan");

			return true;
		}
		sucesores=calculaSucesores(u,muros,stateObs);
		for(int i=0;i<sucesores.size();i++) {
			if (!estado.containsKey(sucesores.get(i).id)) {
				estado.put(sucesores.get(i).id,true);
				sucesores.get(i).padre=u;
				return DFS_search(sucesores.get(i),objetivo,muros,stateObs,estado);
			}
			
		}
		return false;
		
	}
	/**
	 * Funcion para calcular los sucesores.
	 * @param nodo Nodo del que partimos para construir el plan.
	 * @param muros Array con los objetos inmóviles del mapa.
	 * @param cola es la cola dónde tenemos los abiertos actuales.
	 * @param stateObs Observation of the current state.
	 * @return array con los sucesores expandidos.
	 */
	public ArrayList<Nodo> calculaSucesores(Nodo nodo,ArrayList<Nodo> muros, StateObservation stateObs) {
		ArrayList<Nodo> sucesores= new ArrayList<>();
		Nodo sucesor;
		//Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up, newPos_down, newPos_left, newPos_right;
        if (nodo.coordenadas.y - 1 >= 0) {
        	newPos_up = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y-1);	        	
        	sucesor=new Nodo(newPos_up,Types.ACTIONS.ACTION_UP,null);
        	if(!esMuro(sucesor,muros))
        		sucesores.add(sucesor);

        }
        if (nodo.coordenadas.y + 1 <= stateObs.getObservationGrid()[0].length-1) {
        	newPos_down = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y+1);
        	sucesor=new Nodo(newPos_down,Types.ACTIONS.ACTION_DOWN,null);
        	if(!esMuro(sucesor,muros))
        		sucesores.add(sucesor);
        	
        }
        if (nodo.coordenadas.x - 1 >= 0) {
        	newPos_left = new Vector2d(nodo.coordenadas.x - 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_left,Types.ACTIONS.ACTION_LEFT,null);
        	if(!esMuro(sucesor,muros))
        		sucesores.add(sucesor);
        }
        if (nodo.coordenadas.x + 1 <= stateObs.getObservationGrid().length - 1) {
        	newPos_right = new Vector2d(nodo.coordenadas.x + 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_right,Types.ACTIONS.ACTION_RIGHT,null);
        	if(!esMuro(sucesor,muros))
        		sucesores.add(sucesor);
        }		 		
		return sucesores;
	}

	/**
	 * Devuelve si un nodo ha sido o no visitado
	 * @param nodo nodo expandido
	 * @param cola es la cola dónde tenemos los abiertos actuales 
	 * @return devuelve true si es un nodo visitado y false si no
	 */
	private boolean estaVisitado(Nodo sucesor, Queue<Nodo> cola) {
	    Iterator iterator = cola.iterator();
	    
		for(Nodo i : cola) {
			if(i.equals(sucesor))
				return true;
		}
		return false;
	}

	//TODO-Meter los muros en una Tabla Hash
	/**
	 * ver si el un nodo es un muro o pinchos
	 * @param sucesor nodo sexpandido
	 * @param objetos ArrayList con los muros y pinchos
	 * @return devuelve true si es un nodo visitado y false si no
	 */
	private boolean esMuro(Nodo sucesor, ArrayList<Nodo> objetos) {	
		for (int i=0; i<objetos.size();i++) {
			if(objetos.get(i).equals(sucesor))
				return true;
		}
		return false;
	}

}
