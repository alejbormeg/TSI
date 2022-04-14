package tracks.singlePlayer.evaluacion.src_BORREGO_MEGIAS_ALEJANDRO;

import java.util.ArrayList;
import java.util.Hashtable;
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
import tracks.singlePlayer.evaluacion.src_BORREGO_MEGIAS_ALEJANDRO.Nodo;

public class AgenteBFS extends AbstractPlayer{

	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//Pila con el plan a seguir
	private Stack<Types.ACTIONS> plan = new Stack<Types.ACTIONS>();
	
	//Tabla hash con los muros y pinchos en el mapa, usamos esta estructura para acceder en tiempo constante a si una casilla es muro o pincho
	private Hashtable<Double,Boolean> muros_y_pinchos= new Hashtable<Double,Boolean>();	
	//Contador de las llamadas al método act, nodos expandidos y memoria
	int num_llamadas=0;
	int num_nodos_expandidos=0;
	int memoria=0;
	//Nodo inicial y final
	Nodo avatar,portal;
	
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
		double runtime=0.0;
		int tam_plan=0;
		//Si es la primera vez que se llama a act calculamos el plan
        if(num_llamadas==0) {
    		long tInicio = System.nanoTime();
        	//Llamamos al plan con la información del lugar dónde se encuentran los muros
        	plan=planBFS(avatar,portal,muros_y_pinchos,stateObs);
    		long tFin = System.nanoTime();
    		//calculamos tiempo de ejecución
    		runtime += (double)((tFin - tInicio))/1000000;
    		//Calculamos el tamaño del plan
    		tam_plan=plan.size();
        	num_llamadas++;
        	
        	//Mostramos los valores para rellenar la tabla
        	System.out.println("Runtime: "+runtime);
        	System.out.println("Route size: "+tam_plan);
        	System.out.println("Expanded nodes: "+num_nodos_expandidos);
        	System.out.println("Memory: "+memoria);

    		return plan.pop();
        }else { //si no es la primera vez que se llama a act sacamos la siguiente acción de la pila
    		return plan.pop();
        }
        

		
	}

	/**
	 * Devuelve el plan de acción del agente para el resto de ejecuciones.
	 * @param nodo_inicio Nodo del que partimos para construir el plan.
	 * @param nodo_final Nodo al que queremos llegar.
	 * @param muros Array con los objetos inmóviles del mapa.
	 * @param stateObs Observation of the current state.
	 * @return pila con el plan a seguir por el agente.
	 */
	public Stack<Types.ACTIONS> planBFS(Nodo nodo_inicio, Nodo nodo_final,  Hashtable<Double,Boolean> muros,StateObservation stateObs){
		Nodo nodo_actual;
		//Pila con el plan, elegimos la pila como estructura porque las acciones siguen una estructura LIFO
		Stack<Types.ACTIONS> plan= new Stack<Types.ACTIONS>();
		
		//Tabla hash para nodos visitados, elegimos esta estructura porque el acceso se hace en tiempo constante
		Hashtable<Double,Boolean> estado= new Hashtable<Double,Boolean>();
		//Marcamos el nodo inicial como visitado
		estado.put(nodo_inicio.id, true);
		
		//usamos una cola para los abiertos porque vamos a sacar los elementos por el principio siguiendo FIFO
		Queue<Nodo> cola=new LinkedList<>();
		ArrayList<Nodo> sucesores= new ArrayList<>();
		
		//Metemos en la cola el nodo inicial
		cola.add(nodo_inicio);
		
		//mientras la cola tenga elementos
		while(!cola.isEmpty()){
			//tomamos el primero
			nodo_actual=cola.peek();
			cola.remove();
			//lo expandimos
			num_nodos_expandidos++;
			
			//Comprobamos si es el nodo final
			if(nodo_actual.coordenadas.equals(nodo_final.coordenadas)){
				//Si lo es calculamos el camino para llegar hasta él
				plan=nodo_actual.calculaCamino();
				//Vemos el consumo de memoria
				memoria=estado.size();
				//Devolvemos el plan
				return plan;
			}
			
			//Si no es final calculamos sus sucesores
			sucesores=calculaSucesores(nodo_actual,muros,stateObs);
			for(int i=0;i<sucesores.size();i++) {
				//Si no han sido visitados hasta ahora
				if (!estado.containsKey(sucesores.get(i).id)) {
					//los metemos en visitados
					estado.put(sucesores.get(i).id,true);
					sucesores.get(i).padre=nodo_actual;
					//Los añadimos a la cola.
					cola.add(sucesores.get(i));
				}
				
			}
			
		}
		
		return plan;	
	}
	
	/**
	 * Funcion para calcular los sucesores.
	 * @param nodo Nodo del que partimos para construir el plan.
	 * @param muros Array con los objetos inmóviles del mapa.
	 * @param cola es la cola dónde tenemos los abiertos actuales.
	 * @param stateObs Observation of the current state.
	 * @return array con los sucesores expandidos.
	 */
	public ArrayList<Nodo> calculaSucesores(Nodo nodo, Hashtable<Double,Boolean> muros, StateObservation stateObs) {
		//ArrayList dónde metemos los sucesores del nodo que vamos a expandir
		ArrayList<Nodo> sucesores= new ArrayList<>();
		Nodo sucesor;
		//Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up, newPos_down, newPos_left, newPos_right;
        if (nodo.coordenadas.y - 1 >= 0) {
        	newPos_up = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y-1);	        	
        	sucesor=new Nodo(newPos_up,Types.ACTIONS.ACTION_UP,null);
        	//Si no es un muro o pinchos, lo contamos como sucesor
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
	 * Funcion que comprueba si el nodo es un muro o pinchos
	 * @param sucesor nodo sexpandido
	 * @param objetos Tabla Hash con los muros y pinchos
	 * @return devuelve true si es un nodo visitado y false si no
	 */
	private boolean esMuro(Nodo sucesor, Hashtable<Double,Boolean> objetos) {	
		if(objetos.containsKey(sucesor.id))	
			return true;
		else
			return false;
	}

}
