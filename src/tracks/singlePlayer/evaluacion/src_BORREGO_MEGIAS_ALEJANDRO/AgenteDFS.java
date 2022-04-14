package tracks.singlePlayer.evaluacion.src_BORREGO_MEGIAS_ALEJANDRO;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.evaluacion.src_BORREGO_MEGIAS_ALEJANDRO.Nodo;

public class AgenteDFS extends AbstractPlayer {

	Vector2d fescala;
	Vector2d portal_coordenadas;
	
	//Pila con el plan a seguir
	private Stack<Types.ACTIONS> plan = new Stack<Types.ACTIONS>();
	
	//Tabla hash con los muros y pinchos en el mapa, usamos esta estructura para acceder en tiempo constante a si una casilla es muro o pincho
	private Hashtable<Double,Boolean> muros_y_pinchos= new Hashtable<Double,Boolean>();	
	
	//Contador de las llamadas al método act, de nodos expandidos y de memoria
	int num_llamadas=0;
	int nodos_expandidos=0;
	int memoria=0;
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

		//Si es la primera vez que se llama a act calculamos el plan
        if(num_llamadas==0) {
        	
    		double runtime=0.0; //tiempo de ejecución
    		int tam_plan=0; //tamaño del plan
    		
        	//Llamamos al plan con la información del lugar dónde se encuentran los muros
        	num_llamadas++;
        	
    		long tInicio = System.nanoTime();
        	DFS(avatar,portal,muros_y_pinchos,stateObs);
    		long tFin = System.nanoTime();
    		//calculamos tiempo de ejecución
    		runtime += (double)((tFin - tInicio))/1000000;
    		//Calculamos el tamaño del plan
    		tam_plan=plan.size();
    		
    		//Mostramos los valores para rellenar la tabla
        	System.out.println("Runtime: "+runtime);
        	System.out.println("Route size: "+tam_plan);
        	System.out.println("Expanded nodes: "+nodos_expandidos);
        	System.out.println("Memory: "+memoria);
        	
    		return plan.pop();
        }else { //Si no es la primera vez se devuelve la siguiente instrucción en el plan
    		return plan.pop();
        }
        
		
	}
	
	/**
	 * Función que implementa la búsqueda en profundidad para la ruta hasta llegar al objetivo
	 * @param inicial Nodo de inicio
	 * @param objetivo nodo destino
	 * @param muros_y_pinchos2 Conjunto de muros y pinchos del mapa
	 * @param stateObs 
	 */
	public void DFS (Nodo inicial, Nodo objetivo,Hashtable<Double, Boolean> muros_y_pinchos2,StateObservation stateObs) {
		//Tabla hash que usaremos para contabilizar los nodos visitados, usamos esta estructura por su tiempo de acceso constante
		Hashtable<Double,Boolean> estado= new Hashtable<Double,Boolean>();
		//Marcamos el nodo inicial como visitado
		estado.put(inicial.id, true);
		
		//Buscamos en profundidad la ruta desde nodo inicial al objetivo
		DFS_search(inicial,objetivo,muros_y_pinchos2,stateObs,estado);
	}
	
	/**
	 * Función que explora una rama en profundidad de forma recursiva y en caso de llegar al nodo objetivo calcula el plan
	 * @param u Nodo inicio
	 * @param objetivo nodo objetivo
	 * @param muros_y_pinchos2 conjunto de muros y pinchos
	 * @param stateObs
	 * @param estado Tabla con nodos visitados
	 * @return Devuelve true en caso de encontrar la ruta y false en caso contrario
	 */
	public Boolean DFS_search(Nodo u, Nodo objetivo,Hashtable<Double, Boolean> muros_y_pinchos2,StateObservation stateObs,Hashtable<Double,Boolean> estado){
		//ArrayList para almacenar sucesores del nodo expandido
		ArrayList<Nodo> sucesores= new ArrayList<>();
		
		//Contabilizamos nodo expandido
		nodos_expandidos++;
		
		//Si es nodo objetivo
		if(u.equals(objetivo)){
			//Vemos el consumo de memoria
			memoria=estado.size();
			//Calculamos el plan
			plan=u.calculaCamino();
			return true;
		}
		
		//Si no es objetivo calculamos sucesores
		sucesores=calculaSucesores(u,muros_y_pinchos2,stateObs);
		//iteramos sobre los sucesores
		for(int i=0;i<sucesores.size();i++) {
			//Si no ha sido visitado
			if (!estado.containsKey(sucesores.get(i).id)) {
				//Lo marcamos como visitado
				estado.put(sucesores.get(i).id,true);
				//Establecemos el padre de sucesor
				sucesores.get(i).padre=u;
				//LLamamos de nuevo al método pero desde el sucesor hasta el objetivo
				if(DFS_search(sucesores.get(i),objetivo,muros_y_pinchos2,stateObs,estado)) {
					//Si encuentra ruta devolvemos true
					return true;
				}
			}
			
		}
		//Si no encuentra ruta devolvemos false
		return false;
		
	}
	/**
	 * Funcion para calcular los sucesores.
	 * @param nodo Nodo del que partimos para construir el plan.
	 * @param muros_y_pinchos2 Array con los objetos inmóviles del mapa.
	 * @param cola es la cola dónde tenemos los abiertos actuales.
	 * @param stateObs Observation of the current state.
	 * @return array con los sucesores expandidos.
	 */
	public ArrayList<Nodo> calculaSucesores(Nodo nodo,Hashtable<Double, Boolean> muros_y_pinchos2, StateObservation stateObs) {
		//Array con los sucesores del nodo actual
		ArrayList<Nodo> sucesores= new ArrayList<>();
		Nodo sucesor;
		//Probamos las cuatro acciones y calculamos la distancia del nuevo estado al portal.
        Vector2d newPos_up, newPos_down, newPos_left, newPos_right;
        if (nodo.coordenadas.y - 1 >= 0) {
        	newPos_up = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y-1);	        	
        	sucesor=new Nodo(newPos_up,Types.ACTIONS.ACTION_UP,null);
        	if(!esMuro(sucesor,muros_y_pinchos2))
        		sucesores.add(sucesor);

        }
        if (nodo.coordenadas.y + 1 <= stateObs.getObservationGrid()[0].length-1) {
        	newPos_down = new Vector2d(nodo.coordenadas.x, nodo.coordenadas.y+1);
        	sucesor=new Nodo(newPos_down,Types.ACTIONS.ACTION_DOWN,null);
        	if(!esMuro(sucesor,muros_y_pinchos2))
        		sucesores.add(sucesor);
        	
        }
        if (nodo.coordenadas.x - 1 >= 0) {
        	newPos_left = new Vector2d(nodo.coordenadas.x - 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_left,Types.ACTIONS.ACTION_LEFT,null);
        	if(!esMuro(sucesor,muros_y_pinchos2))
        		sucesores.add(sucesor);
        }
        if (nodo.coordenadas.x + 1 <= stateObs.getObservationGrid().length - 1) {
        	newPos_right = new Vector2d(nodo.coordenadas.x + 1, nodo.coordenadas.y);
        	sucesor=new Nodo(newPos_right,Types.ACTIONS.ACTION_RIGHT,null);
        	if(!esMuro(sucesor,muros_y_pinchos2))
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
