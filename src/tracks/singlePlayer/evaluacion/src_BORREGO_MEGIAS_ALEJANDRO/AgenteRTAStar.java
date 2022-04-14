package tracks.singlePlayer.evaluacion.src_BORREGO_MEGIAS_ALEJANDRO;

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
import tracks.singlePlayer.evaluacion.src_BORREGO_MEGIAS_ALEJANDRO.Nodo;

public class AgenteRTAStar extends AbstractPlayer{
	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//Tabla Hash con los muros y pinchos en el mapa para acceder a ellos en tiempo constante
	Hashtable<Double,Boolean> muros_y_pinchos= new Hashtable<Double,Boolean>();
	
	//Tabla Hash para actualizar las h(n) en el algoritmo en tiempo constante
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
	
	/**
	 * return the best action to arrive faster to the closest portal
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return best	ACTION to arrive faster to the closest portal
	 */
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		//medimos el tiempo de ejecución del algoritmo
		long tInicio = System.nanoTime();
		Types.ACTIONS accion=RTA(avatar,muros_y_pinchos,stateObs);
		long tFin = System.nanoTime();
		//Lo vamos acumulando
		runtime += (double)((tFin - tInicio))/1000000;
		tam_plan++;
		//Solo cuando el avatar llega al portal mostramos las estadísticas
		if(avatar.equals(portal)) {
			System.out.println("Runtime: "+runtime);
			System.out.println("Route size: "+tam_plan); 
			System.out.println("Expanded Nodes: "+tam_plan); //Coincide con la ruta en este caso
			System.out.println("Memory: "+nodos.size());
		}
		//Realizamos la acción devuelta por el algoritmo
		return accion;
	}
	
	
	/**
	 * Algoritmo RTA*
	 * @param n_actual Nodo en el que el avatar se encuentra actualmente
	 * @param muros_y_pinchos2 Tabla hash con muros y pinchos del mapa
	 * @param stateObs
	 * @return Devuelve la acción a realizar para llegar al portal
	 */
	private ACTIONS RTA(Nodo n_actual, Hashtable<Double, Boolean> muros_y_pinchos2,
			StateObservation stateObs) {
		//Usamos una cola con prioridad que ordene los vecinos por la f de menor a mayor
		PriorityQueue<Nodo> vecinos= calculaSucesores(n_actual,muros_y_pinchos2,stateObs);
		//El siguiente nodo al que nos movemos es el primero de la cola
		Nodo siguiente_nodo=vecinos.peek();
		
		if(!nodos.containsKey(n_actual.id)) {
			//Aplicamos la regla de aprendizaje al nodo actual
			nodos.put(n_actual.id, reglaAprendizaje(n_actual.h,vecinos));
		}else {
			//Actualizamos la h del nodo 
			nodos.replace(n_actual.id, nodos.get(n_actual.id), reglaAprendizaje(nodos.get(n_actual.id),vecinos));
		}
		//El avatar avanza al nodo seleccionado
		avatar=new Nodo(siguiente_nodo);
		//Devolvemos la acción a realizar
		return siguiente_nodo.accion_desde_padre;
	}
	
	/**
	 * Implementa la regla de aprendizaje del segundo mínimo del algoritmo RTA*
	 * @param h
	 * @param vecinos
	 * @return
	 */
	private Double reglaAprendizaje(double h, PriorityQueue<Nodo> vecinos) {
		//Tomamos en primer lugar el primer mínimo 
		double segundo_minimo=vecinos.poll().f;
		
		//Si no hubiese más elementos en los vecinos, devolvemos el primer mínimo
		if(!vecinos.isEmpty()) {
			//Si hay más elementos el segundo mínimo es el del que está actualmente primero en la cola de prioridad
			segundo_minimo=vecinos.poll().f;
		}
		
		//devolvemos el máximo entre la h que teníamos y el segundo mínimo
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
		//Usamos una cola con prioridad para tenerlos ordenados de menor a mayor por las f
		PriorityQueue<Nodo> sucesores= new PriorityQueue<>();
		Nodo sucesor;
		//Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up, newPos_down, newPos_left, newPos_right;
        
        //Repetimos la misma idea para todo hijo del nodo actual
        if (nodo.coordenadas.y - 1 >= 0) {
        	newPos_up = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y-1);	
        	sucesor=new Nodo(newPos_up,Types.ACTIONS.ACTION_UP, nodo);
        	//Si no lo tenemos en la tabla hash de nodos actualizados calculamos su distancia manhattan al objetivo
        	if(!nodos.containsKey(sucesor.id)) {
        		sucesor.g=1;
        		sucesor.h=Manhattan(newPos_up,portal.coordenadas);
        		sucesor.f=sucesor.g+sucesor.h;
        	}else { //Si lo tenemos en la tabla de nodos usamos la h que tengamos almacenada para ese nodo
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
