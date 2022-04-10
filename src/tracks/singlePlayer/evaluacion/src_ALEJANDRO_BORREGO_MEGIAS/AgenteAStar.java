package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import core.player.AbstractPlayer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS.Nodo;

public class AgenteAStar extends AbstractPlayer {	
	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//ArrayList con el plan a seguir
	private Stack<Types.ACTIONS> plan = new Stack<Types.ACTIONS>();
	
	//ArrayList con los muros y pinchos en el mapa
	Hashtable<Double,Boolean> muros_y_pinchos= new Hashtable<Double,Boolean>();
	
	//Contador de las llamadas al método act
	int num_llamadas=0;
	
	//Nodo inicial y final
	Nodo avatar,portal;
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
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
            muros_y_pinchos.put( new Nodo(new Vector2d(Math.floor(obstaculos[0].get(i).position.x / fescala.x), Math.floor(obstaculos[0].get(i).position.y / fescala.y))).id,true);
        }
        
        for (int i = 0; i < obstaculos[1].size(); i++){
            //Obtenemos la posición de cada uno
            muros_y_pinchos.put( new Nodo(new Vector2d(Math.floor(obstaculos[1].get(i).position.x / fescala.x), Math.floor(obstaculos[1].get(i).position.y / fescala.y))).id,true);
        }
        
      //Posicion del avatar en coordenadas
        Vector2d pos_avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x, 
        		stateObs.getAvatarPosition().y / fescala.y);
      //Pareja, posición/estado del avatar
        
        avatar=new Nodo(pos_avatar,0,Manhattan(pos_avatar,portal.coordenadas));
        avatar.padre=avatar;
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
        	ASTAR(avatar,portal,muros_y_pinchos,stateObs);
    		return plan.pop();
        }else {
    		return plan.pop();
        }
        
		
	}
	
	
	public void ASTAR(Nodo nodo_inicial, Nodo nodo_objetivo,Hashtable<Double,Boolean> muros, StateObservation stateObs) {
		TreeSet<Nodo> abiertos=new TreeSet<Nodo>(); //TODO Si algo falla revisar esto
		Hashtable<Double,Double> cerrados= new Hashtable<Double,Double>(); //Nodos ya visitados (pareja id-f)
		abiertos.add(nodo_inicial);
		Nodo nodo_actual;
		
		while (true) {
			nodo_actual=abiertos.first();
			if (nodo_actual.equals(nodo_objetivo)) {
				avatar.padre=null;
				break;
			}
			abiertos.pollFirst();
			
			for(Nodo sucesor: calculaSucesores(nodo_actual,muros,stateObs,nodo_objetivo)) {
				if (!sucesor.equals(nodo_actual.padre)) { 
					if (cerrados.containsKey(sucesor.id) && cerrados.get(sucesor.id)<sucesor.f) {
						cerrados.remove(sucesor.id);
						abiertos.add(sucesor);
					}else if (!cerrados.containsKey(sucesor.id) && !estaEnAbiertos(sucesor,abiertos)) {
						abiertos.add(sucesor);
					}else if (estaEnAbiertos(sucesor,abiertos)) { // se implementa igual que en el pseudocódigo, epro para evitar repetir operaciones se hace diferente
					mejoraCamino(sucesor,abiertos);
					}
				}
				
			}
			cerrados.put(nodo_actual.id,nodo_actual.f);
			
		}

		plan=nodo_actual.calculaCamino();
		
	}
	
	/**
	 * Comprueba si el camino desde inicio a sucesor es mejor que que había en abiertos
	 * @param sucesor
	 * @param abiertos
	 * @return true si mejora y false en caso contrario
	 */
	private void mejoraCamino(Nodo sucesor, TreeSet<Nodo> abiertos) {
		
		for(Nodo n: abiertos) {
			//Si está en abiertos y mejora camino actualizamos
			if(n.equals(sucesor)&& n.f>sucesor.f) {
				n.g=sucesor.g;
			}
		}
	}

	/**
	 * Mira si un nodo está en la lista de abiertos
	 * @param sucesor nodo a comprobar
	 * @param abiertos conjunto de abiertos
	 * @return true si está y false en caso contrario
	 */
	private boolean estaEnAbiertos(Nodo sucesor, TreeSet<Nodo> abiertos) {		
		for(Nodo n: abiertos) {
			if(n.id==sucesor.id) {
				return true;
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
	public ArrayList<Nodo> calculaSucesores(Nodo nodo,Hashtable<Double,Boolean> muros, StateObservation stateObs, Nodo objetivo) {
		ArrayList<Nodo> sucesores= new ArrayList<>();
		Nodo sucesor;
		//Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up, newPos_down, newPos_left, newPos_right;
        if (nodo.coordenadas.y - 1 >= 0) {
        	newPos_up = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y-1);	        	
        	sucesor=new Nodo(newPos_up,Types.ACTIONS.ACTION_UP,nodo,nodo.g+1,Manhattan(newPos_up,objetivo.coordenadas));
        	if(!muros.containsKey(sucesor.id))
        		sucesores.add(sucesor);

        }
        if (nodo.coordenadas.y + 1 <= stateObs.getObservationGrid()[0].length-1) {
        	newPos_down = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y+1);
        	sucesor=new Nodo(newPos_down,Types.ACTIONS.ACTION_DOWN,nodo,nodo.g+1,Manhattan(newPos_down,objetivo.coordenadas));
        	if(!muros.containsKey(sucesor.id))
        		sucesores.add(sucesor);
        	
        }
        if (nodo.coordenadas.x - 1 >= 0) {
        	newPos_left = new Vector2d(nodo.coordenadas.x - 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_left,Types.ACTIONS.ACTION_LEFT,nodo,nodo.g+1,Manhattan(newPos_left,objetivo.coordenadas));
        	if(!muros.containsKey(sucesor.id))
        		sucesores.add(sucesor);
        }
        if (nodo.coordenadas.x + 1 <= stateObs.getObservationGrid().length - 1) {
        	newPos_right = new Vector2d(nodo.coordenadas.x + 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_right,Types.ACTIONS.ACTION_RIGHT,nodo,nodo.g+1,Manhattan(newPos_right,objetivo.coordenadas));
        	if(!muros.containsKey(sucesor.id))
        		sucesores.add(sucesor);
        }		 		
		return sucesores;
	}
	
	/**
	 * Calcula la distancia Manhattan entre el nodo actual y el objetivo.
	 * @param nodo_actual
	 * @param nodo_objetivo
	 * @return Distancia Manhattan entre ambos
	 */
	public double Manhattan(Vector2d coord_actual, Vector2d coord_dest) {
		return (double) (Math.abs(coord_actual.x - coord_dest.x)+Math.abs(coord_actual.y - coord_dest.y));
		
	}

}
