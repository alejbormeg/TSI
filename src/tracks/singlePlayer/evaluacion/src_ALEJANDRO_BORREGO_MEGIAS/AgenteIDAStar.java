package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import core.player.AbstractPlayer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
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

public class AgenteIDAStar extends AbstractPlayer{
	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//ArrayList con el plan a seguir
	private Stack<Types.ACTIONS> plan = new Stack<Types.ACTIONS>();
	
	//ArrayList con los muros y pinchos en el mapa
	Hashtable<Double,Boolean> muros_y_pinchos= new Hashtable<Double,Boolean>();
	
	//Contador de las llamadas al método act
	int num_llamadas=0;
	int nodos_expandidos=0; //Contador de nodos expandidos 

	//Nodo inicial y final
	Nodo avatar,portal;
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteIDAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
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

        if(num_llamadas==0) {
        	//Llamamos al plan con la información del lugar dónde se encuentran los muros
        	num_llamadas++;
        	plan=IDASTAR(avatar,portal,muros_y_pinchos,stateObs);
    		return plan.pop();
        }else if (!plan.isEmpty()){
    		return plan.pop();
        }else
        	return Types.ACTIONS.ACTION_ESCAPE;
        
		
	}
	
	/**
	 * Algoritmo IDA*, calcula la ruta a seguir por el avatar y lo almacena en la pila plan
	 * @param nodo_inicial nodo de partida (pos inicial del avatar)
	 * @param nodo_objetivo nodo objetivo (portal)
	 * @param muros Tabla hash con los muros y pinchos del mapa
	 * @param stateObs Para acceder a información del mapa
	 */
	public Stack<Types.ACTIONS> IDASTAR(Nodo nodo_inicial, Nodo nodo_objetivo,Hashtable<Double,Boolean> muros, StateObservation stateObs) {

		Stack<Types.ACTIONS> acciones=new Stack<>();
		int cota=(int) nodo_inicial.h; //Cota inicial
		int t;
		
		LinkedList<Nodo>ruta=new LinkedList <>();
		ruta.addFirst(nodo_inicial);
		
		//Comienza el algoritmo
		while (true) {
			t=search(ruta,0,cota,nodo_objetivo,muros,stateObs);	
			if(t==-1) {
				System.out.println("Nodos expandidos: "+nodos_expandidos);
				System.out.println("Consumo de memoria: "+ruta.size());
				acciones=ruta.getLast().calculaCamino();
				return acciones;
			}	
			if(t==Integer.MAX_VALUE) {
				System.out.println("No se ha encontrado el plan, devolvemos NIL");
				acciones.add(Types.ACTIONS.ACTION_NIL);
				return acciones;
			}	
			cota=t; //Actualizamos la profundidad máxima
		}

	}
	
	/**
	 * Método para desarrollar una ruta en el algoritmo IDA*
	 * @param ruta Ruta actual
	 * @param g Valor de g actual
	 * @param cota Cota actual
	 * @param nodo_objetivo Objetivo
	 * @param muros muros y pinchos del mapa
	 * @param stateObs 
	 * @return Devuelve -1 si encontramos objetivo, infinito si no y un entero f en caso de sobrepasar la cota.
	 */
	private int search(LinkedList<Nodo> ruta, int g, int cota, Nodo nodo_objetivo,Hashtable<Double,Boolean> muros,StateObservation stateObs) {
		Nodo nodo,sucesor;
		PriorityQueue<Nodo> sucesores;
		nodo=ruta.getLast();

		int t;
		
		int f=(int) (g+nodo.h);
		
		if(f>cota) return f;
		
		nodos_expandidos++;
		if(nodo.equals(nodo_objetivo)) {
			return -1;
		}
		
		int min=Integer.MAX_VALUE;
		
		sucesores=calculaSucesores(nodo,muros,stateObs,nodo_objetivo);
		while(!sucesores.isEmpty()) {
			sucesor=sucesores.poll();
			if(!ruta.contains(sucesor)) {
				ruta.addLast(sucesor);
				t=search(ruta,g+1,cota,nodo_objetivo,muros,stateObs);
				if(t==-1) return -1;
				if(t<min) min=t;
				ruta.removeLast();
			}
		}
		return min;
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
	public PriorityQueue<Nodo> calculaSucesores(Nodo nodo,Hashtable<Double,Boolean> muros, StateObservation stateObs, Nodo objetivo) {
		PriorityQueue<Nodo> sucesores= new PriorityQueue<>();
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
