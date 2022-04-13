package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import core.player.AbstractPlayer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Stack;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS.Nodo;

public class AgenteRTAStar extends AbstractPlayer{
	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//Tabla Hash con los muros y pinchos en el mapa
	Hashtable<Double,Boolean> muros_y_pinchos= new Hashtable<Double,Boolean>();
	
	//Tabla Hash para actualizar las h(n) en el algoritmo
	Hashtable<Double,Double> nodos= new Hashtable<Double,Double>();
	
	//Contador de las llamadas al método act
	int nodos_expandidos=0; //Contador de nodos expandidos 
	int tam_plan=0;
	double runtime=0;
	//Nodo inicial y final
	Nodo avatar,portal;
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
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
	}
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		long tInicio = System.nanoTime();
		Types.ACTIONS accion=RTA(avatar,muros_y_pinchos,stateObs);
		long tFin = System.nanoTime();
		
		runtime += (double)((tFin - tInicio))/1000000;
		tam_plan++;
		if(avatar.equals(portal)) {
			System.out.println("Runtime: "+runtime);
			System.out.println("Route size: "+tam_plan);
			System.out.println("Expanded Nodes: "+tam_plan);
			System.out.println("Memory: "+nodos.size());
		}
		return accion;
	}
	
	
	
	private ACTIONS RTA(Nodo n_actual, Hashtable<Double, Boolean> muros_y_pinchos2,
			StateObservation stateObs) {
		
		PriorityQueue<Nodo> vecinos= calculaSucesores(n_actual,muros_y_pinchos2,stateObs);
		Nodo siguiente_nodo=vecinos.peek();
		
		//System.out.println("nos movemos a "+ siguiente_nodo.coordenadas.toString()+" con f:"+siguiente_nodo.f);
		if(!nodos.containsKey(n_actual.id)) {
			nodos.put(n_actual.id, reglaAprendizaje(n_actual.h,vecinos));
		}else {
			nodos.replace(n_actual.id, nodos.get(n_actual.id), reglaAprendizaje(nodos.get(n_actual.id),vecinos));
		}
		
		avatar=new Nodo(siguiente_nodo);
		return siguiente_nodo.accion_desde_padre;
	}
	
	/**
	 * Iplementa la regla de aprendizaje del segundo mínimo del algoritmo RTA*
	 * @param h
	 * @param vecinos
	 * @return
	 */
	private Double reglaAprendizaje(double h, PriorityQueue<Nodo> vecinos) {
		double segundo_minimo=vecinos.poll().f;
		
		if(!vecinos.isEmpty()) {
			segundo_minimo=vecinos.poll().f;
		}

		return Math.max(h,segundo_minimo);
	}
	
	/**
	 * Funcion para calcular los sucesores.
	 * @param nodo Nodo del que partimos para construir el plan.
	 * @param muros Array con los objetos inmóviles del mapa.
	 * @param stateObs Observation of the current state.
	 * @param objetivo Nodo objetivo
	 * 
	 * @return Lista con prioridad de los sucesores expandidos.
	 */
	public PriorityQueue<Nodo> calculaSucesores(Nodo nodo,Hashtable<Double,Boolean> muros, StateObservation stateObs) {
		PriorityQueue<Nodo> sucesores= new PriorityQueue<>();
		Nodo sucesor;
		//Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up, newPos_down, newPos_left, newPos_right;
        
        if (nodo.coordenadas.y - 1 >= 0) {
        	newPos_up = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y-1);	
        	sucesor=new Nodo(newPos_up,Types.ACTIONS.ACTION_UP, nodo);
        	if(!nodos.containsKey(sucesor.id)) {
        		sucesor.g=1;
        		sucesor.h=Manhattan(newPos_up,portal.coordenadas);
        		sucesor.f=sucesor.g+sucesor.h;
        	}else {
        		sucesor.g=1;
        		sucesor.h=nodos.get(sucesor.id);
        		sucesor.f=sucesor.g+sucesor.h;
        	}
        	if(!muros.containsKey(sucesor.id))
        		sucesores.add(sucesor);

        }
        if (nodo.coordenadas.y + 1 <= stateObs.getObservationGrid()[0].length-1) {
        	newPos_down = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y+1);
        	sucesor=new Nodo(newPos_down,Types.ACTIONS.ACTION_DOWN, nodo);
        	if(!nodos.containsKey(sucesor.id)) {
        		sucesor.g=1;
        		sucesor.h=Manhattan(newPos_down,portal.coordenadas);
        		sucesor.f=sucesor.g+sucesor.h;
        	}else {
        		sucesor.g=1;
        		sucesor.h=nodos.get(sucesor.id);
        		sucesor.f=sucesor.g+sucesor.h;
        	}        	if(!muros.containsKey(sucesor.id))
        		sucesores.add(sucesor);
        	        	
        }
        if (nodo.coordenadas.x - 1 >= 0) {
        	newPos_left = new Vector2d(nodo.coordenadas.x - 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_left,Types.ACTIONS.ACTION_LEFT, nodo);
        	if(!nodos.containsKey(sucesor.id)) {
        		sucesor.g=1;
        		sucesor.h=Manhattan(newPos_left,portal.coordenadas);
        		sucesor.f=sucesor.g+sucesor.h;
        	}else {
        		sucesor.g=1;
        		sucesor.h=nodos.get(sucesor.id);
        		sucesor.f=sucesor.g+sucesor.h;
        	}
        	if(!muros.containsKey(sucesor.id))
        		sucesores.add(sucesor);
        	
        }
        if (nodo.coordenadas.x + 1 <= stateObs.getObservationGrid().length - 1) {
        	newPos_right = new Vector2d(nodo.coordenadas.x + 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_right,Types.ACTIONS.ACTION_RIGHT, nodo);
        	if(!nodos.containsKey(sucesor.id)) {
        		sucesor.g=1;
        		sucesor.h=Manhattan(newPos_right,portal.coordenadas);
        		sucesor.f=sucesor.g+sucesor.h;
        	}else {
        		sucesor.g=1;
        		sucesor.h=nodos.get(sucesor.id);
        		sucesor.f=sucesor.g+sucesor.h;
        	}
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
