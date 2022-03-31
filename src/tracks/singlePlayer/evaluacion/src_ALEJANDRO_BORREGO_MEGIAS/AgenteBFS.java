package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import java.util.ArrayList;
import java.util.Collections;
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
import tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS.Pair;
import tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS.Nodo;

public class AgenteBFS extends AbstractPlayer{
	//Greedy Camel: 
	// 1) Busca la puerta más cercana. 
	// 2) Escoge la accion que minimiza la distancia del camello a la puerta.

	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//ArrayList con el plan a seguir
	private Stack<Types.ACTIONS> plan = new Stack<Types.ACTIONS>();
	
	//ArrayList con los muros en el mapa
	private ArrayList<Nodo> muros = new ArrayList<>();

	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteBFS(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
		//Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length , 
        		stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);      
      
        //Se crea una lista de observaciones de portales, ordenada por cercania al avatar
        ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
        //Seleccionamos coordenadas del Portal
        portal_coordenadas = posiciones[0].get(0).position;
        portal_coordenadas.x = Math.floor(portal_coordenadas.x / fescala.x);
        portal_coordenadas.y = Math.floor(portal_coordenadas.y / fescala.y);
        
        // Definimos para los nodos la pareja (coordenadas, estado).
        Nodo portal = new Nodo(portal_coordenadas,false,null,null);

        //Obtenemos las posiciones de los muros
        ArrayList<Observation>[] obstaculos = stateObs.getImmovablePositions();
        System.out.println("Numero de elementos inmoviles: "); //No nos da las posiciones de los pinchos.
        System.out.println(obstaculos[0].size());
        for (int i = 0; i < obstaculos[0].size(); i++){
            //Obtenemos la posición de cada uno
            muros.add( new Nodo(new Vector2d(Math.floor(obstaculos[0].get(i).position.x / fescala.x), Math.floor(obstaculos[0].get(i).position.y / fescala.y)),true,null,null));
        }
        
      //Posicion del avatar en coordenadas
        Vector2d pos_avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x, 
        		stateObs.getAvatarPosition().y / fescala.y);
      //Pareja, posición/estado del avatar
        Nodo avatar=new Nodo(pos_avatar, false,null,null);

        
        //Llamamos al plan con la información del lugar dónde se encuentran los muros
        plan=planBFS(avatar,portal,muros,stateObs);
        
	}

	/**
	 * return the best action to arrive faster to the closest portal
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return best	ACTION to arrive faster to the closest portal
	 */
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
		return plan.pop();
        
        /**
        //Manhattan distance (La heurística)
        ArrayList<Integer> distances = new ArrayList<Integer>();
        distances.add((int) (Math.abs(newPos_up.x - portal.x) + Math.abs(newPos_up.y-portal.y)));
        distances.add((int) (Math.abs(newPos_down.x - portal.x) + Math.abs(newPos_down.y-portal.y)));
        distances.add((int) (Math.abs(newPos_left.x - portal.x) + Math.abs(newPos_left.y-portal.y)));
        distances.add((int) (Math.abs(newPos_right.x - portal.x) + Math.abs(newPos_right.y-portal.y)));     
        
        // Nos quedamos con el menor y tomamos esa accion. 
        int minIndex = distances.indexOf(Collections.min(distances));
       */
         
		
	}

	
	public Stack<Types.ACTIONS> planBFS(Nodo nodo_inicio, Nodo nodo_final, ArrayList<Nodo> muros,StateObservation stateObs){
		Nodo nodo_actual;
		Stack<Types.ACTIONS> plan= new Stack<Types.ACTIONS>();
		//Marcamos el nodo inicial como visitado
		nodo_inicio.estado=true;
		Queue<Nodo> cola=new LinkedList<>();
		ArrayList<Nodo> sucesores= new ArrayList<>();
		//ArrayList<Nodo> visitados= new ArrayList<>();
		
		System.out.println("Comenzamos a buscar");
		//Metemos en la cola el nodo inicial
		cola.add(nodo_inicio);
		//Añadimos a visitados el nodo inicial
		//visitados.add(nodo_inicio);
		while(!cola.isEmpty()){
			nodo_actual=cola.peek();
			cola.remove();
			
			if(nodo_actual.coordenadas.equals(nodo_final.coordenadas)){
				System.out.println("calculamos el plan");
				return nodo_actual.calculaCamino();
			}
			
			sucesores=calculaSucesores(nodo_actual,muros,cola,stateObs);

			for(int i=0;i<sucesores.size();i++) {
				if (sucesores.get(i).estado==false) {
					sucesores.get(i).estado=true;
					sucesores.get(i).padre=nodo_actual;
					//visitados.add(sucesores.get(i));
					cola.add(sucesores.get(i));
				}
				
			}
			
		}
		
		return plan;	
	}

	public ArrayList<Nodo> calculaSucesores(Nodo nodo,ArrayList<Nodo> muros, Queue<Nodo> cola, StateObservation stateObs) {
		ArrayList<Nodo> sucesores= new ArrayList<>();
		Nodo sucesor;
		//Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up, newPos_down, newPos_left, newPos_right;
        if (nodo.coordenadas.y - 1 >= 0) {
        	newPos_up = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y-1);	        	
        	sucesor=new Nodo(newPos_up, false,Types.ACTIONS.ACTION_UP,null);
        	if(!esMuro(sucesor,muros) && estaVisitado(sucesor,cola)==false)
        		sucesores.add(sucesor);

        }
        if (nodo.coordenadas.y + 1 <= stateObs.getObservationGrid()[0].length-1) {
        	newPos_down = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y+1);
        	sucesor=new Nodo(newPos_down, false,Types.ACTIONS.ACTION_DOWN,null);
        	if(!esMuro(sucesor,muros) && estaVisitado(sucesor,cola)==false)
        		sucesores.add(sucesor);
        	
        }
        if (nodo.coordenadas.x - 1 >= 0) {
        	newPos_left = new Vector2d(nodo.coordenadas.x - 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_left, false,Types.ACTIONS.ACTION_LEFT,null);
        	if(!esMuro(sucesor,muros) && estaVisitado(sucesor,cola)==false)
        		sucesores.add(sucesor);
        }
        if (nodo.coordenadas.x + 1 <= stateObs.getObservationGrid().length - 1) {
        	newPos_right = new Vector2d(nodo.coordenadas.x + 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_right, false,Types.ACTIONS.ACTION_RIGHT,null);
        	if(!esMuro(sucesor,muros) && estaVisitado(sucesor,cola)==false)
        		sucesores.add(sucesor);
        }		 		
		return sucesores;
	}

	private boolean estaVisitado(Nodo sucesor, Queue<Nodo> cola) {
	    Iterator iterator = cola.iterator();
	    
		for(Nodo i : cola) {
			if(i.equals(sucesor))
				return true;
		}
		return false;
	}

	private boolean esMuro(Nodo sucesor, ArrayList<Nodo> muros2) {	
		for (int i=0; i<muros2.size();i++) {
			if(muros2.get(i).equals(sucesor))
				return true;
		}
		return false;
	}
}
