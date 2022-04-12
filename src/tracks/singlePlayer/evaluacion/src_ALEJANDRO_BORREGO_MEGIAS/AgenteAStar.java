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
        }else if (!plan.isEmpty()){
    		return plan.pop();
        }else
        	return Types.ACTIONS.ACTION_ESCAPE;
        
		
	}
	
	/**
	 * Algoritmo A*, calcula la ruta a seguir por el avatar y lo almacena en la pila plan
	 * @param nodo_inicial nodo de partida (pos inicial del avatar)
	 * @param nodo_objetivo nodo objetivo (portal)
	 * @param muros Tabla hash con los muros y pinchos del mapa
	 * @param stateObs Para acceder a información del mapa
	 */
	public void ASTAR(Nodo nodo_inicial, Nodo nodo_objetivo,Hashtable<Double,Boolean> muros, StateObservation stateObs) {
		//Para mantener una lista de nodos ordenados por la f usaremos una cola con prioridad
		PriorityQueue<Nodo> abiertos= new PriorityQueue<Nodo>();
		//Para los accesos y actualizaciones a abiertos usaremos una tabla Hash
		Hashtable<Double,Double> abiertos_auxiliar= new Hashtable<Double,Double>(); 

		//los cerrados los metemos también en una tabla hash
		Hashtable<Double,Double> cerrados= new Hashtable<Double,Double>(); 
		
		//ntroducimos el nodo inicial en la tabla hash y cola de abiertos
		abiertos.add(nodo_inicial);
		abiertos_auxiliar.put(nodo_inicial.id, nodo_inicial.g);
		
		//Variables que usaremos en el algoritmo
		Nodo nodo_actual; //Para iterar sobre la cola de abiertos
		int nodos_expandidos=0; //Contador de nodos expandidos 
		int memoria_max=0; //Contador de nodos en memoria máxima
		int memoria=0;
		
		//Comienza el algoritmo
		while (true) {
			nodo_actual=abiertos.poll(); //tomamos el elemento de la cola con menor f
			
			//Comprobamos si el valor de g que tiene la cola está actualizado con el de la tabla hash
			if(!esNodoCorrecto(nodo_actual,abiertos_auxiliar,abiertos)) {
				// Si no estaba actualizado lo actualizamos y pasamos al siguiente en abiertos
				break;
			}
			
			//eliminamos el nodo de la tanbla hash
			abiertos_auxiliar.remove(nodo_actual.id);
			nodos_expandidos++;

			if (nodo_actual.equals(nodo_objetivo)) {
				avatar.padre=null;
				break;
			}
			
			//Calculamos los sucesores e iteramos sobre ellos
			for(Nodo sucesor:calculaSucesores(nodo_actual,muros,stateObs,nodo_objetivo)) {
				//Primero nos aseguramos de que el nodo abuelo no coincida con el sucesor
				if (!sucesor.equals(nodo_actual.padre)) { 
					//Si el sucesor estaba en cerrados pero mejora su g lo rescatamos 
					if (cerrados.containsKey(sucesor.id) && cerrados.get(sucesor.id)>sucesor.g) {
						cerrados.remove(sucesor.id);
						abiertos.add(sucesor);
						abiertos_auxiliar.put(sucesor.id, sucesor.g);
					//Si no estaba en cerrados ni en abiertos lo metemos en abiertos
					}else if (!cerrados.containsKey(sucesor.id) && !abiertos_auxiliar.containsKey(sucesor.id)) {
						abiertos.add(sucesor);
						abiertos_auxiliar.put(sucesor.id,sucesor.g);
					//Si estaba en abiertos pero mejoramos su g actual la cambiamos
					}else if (abiertos_auxiliar.containsKey(sucesor.id) && abiertos_auxiliar.get(sucesor.id)>sucesor.g) { // se implementa igual que en el pseudocódigo, epro para evitar repetir operaciones se hace diferente
						abiertos_auxiliar.replace(sucesor.id, abiertos_auxiliar.get(sucesor.id), sucesor.g);
					}
				}
				
			}
			
			//Metemos el nodo expandido en cerrados
			cerrados.put(nodo_actual.id,nodo_actual.g);
			
			//Comprobamos si alcanzamos un maximo en nodos en memoria
			memoria=cerrados.size()+abiertos_auxiliar.size();
			if(memoria>memoria_max) {
				memoria_max=memoria;
			}

		}
		System.out.println("Total de nodos expandidos: "+nodos_expandidos);
		System.out.print("Consumo en memoria: ");
		System.out.print(memoria_max);
		System.out.println();

		plan=nodo_actual.calculaCamino();
	}
	

	/**
	 * Comprueba si el nodo que sacamos de abiertos coincide en la g con el nodo correspondiente en la tabla hash auxiliar
	 * @param abiertos cola con prioridad actual de abiertos
	 * @param nodo_actual Nodo que queremos sacar de abiertos
	 * @param abiertos_auxiliar Tabla Hash con las g actualizadas
	 * @return true si es correcto y false si no
	 */
	private boolean esNodoCorrecto(Nodo nodo_actual, Hashtable<Double, Double> abiertos_auxiliar, PriorityQueue<Nodo> abiertos){
		if (abiertos_auxiliar.get(nodo_actual.id)!=nodo_actual.g) {
			System.out.println("g del nodo actual: "+ nodo_actual.g + " g de la tabla hash: "+abiertos_auxiliar.get(nodo_actual.id));
		}
		
		if(!abiertos_auxiliar.containsKey(nodo_actual.id))
			return false;
		else if (abiertos_auxiliar.get(nodo_actual.id)==nodo_actual.g)
			return true;
		else {
			nodo_actual.g=abiertos_auxiliar.get(nodo_actual.id);
			nodo_actual.f=nodo_actual.h+nodo_actual.g;
			abiertos.add(nodo_actual); //Lo colocamos en su lugar correspondiente en la cola ordenada
			return false;
		}
		
	}

	/**
	 * Funcion para calcular los sucesores.
	 * @param nodo Nodo del que partimos para construir el plan.
	 * @param muros Array con los objetos inmóviles del mapa.
	 * @param stateObs Observation of the current state.
	 * @param objetivo Nodo objetivo
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
