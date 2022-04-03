package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import java.util.Stack;

import ontology.Types;
import tools.Vector2d;

public class Nodo {
	public Vector2d coordenadas; //Coordenadas 2d del nodo
	public Types.ACTIONS accion_desde_padre; //Acción realizada desde el padre para llegar al nodo
	public Nodo padre; //puntero al nodo padre
	public double id; //Identificador único
	
	/**
	 * Constructor con parámetros
	 * @param coord coordenadas 2d
	 * @param accion Acción realizada desde el padre
	 * @param padre Nodo padre
	 */
	public Nodo(Vector2d coord, Types.ACTIONS accion, Nodo padre) {
		this.coordenadas=coord;
		this.accion_desde_padre=accion;
		this.padre=padre;
		this.id=10000*this.coordenadas.x+coordenadas.y;
	}
	/**
	 * Constructor con parámetros
	 * @param coordenadas
	 */
	public Nodo(Vector2d coordenadas) {
		this.coordenadas=coordenadas;
		this.accion_desde_padre=Types.ACTIONS.ACTION_NIL;
		this.padre=null;
		this.id=10000*this.coordenadas.x+coordenadas.y;
	}
	
	/**
	 * Constructor copia
	 * @param n nodo del que vamos a hacer la copia
	 */
	public Nodo(Nodo n) {
		this.coordenadas=n.coordenadas;
		this.accion_desde_padre=n.accion_desde_padre;
		this.padre=n.padre;
		this.id=n.id;
	}
	
	
	
	/**
	 * Método equals para ver si dos nodos son iguales
	 * @param n nodo con el que comparar
	 * @return true si sus id coinciden y false en otro caso
	 */
	public Boolean equals(Nodo n) {
		if(this.id==n.id)
			return true;
		else
			return false;
	}
	
	/**
	 * Calcula el camino desde el nodo raíz hasta el actual
	 * @return pila de acciones a seguir para llegar al nodo actual
	 */
	public Stack<Types.ACTIONS> calculaCamino(){
		Stack<Types.ACTIONS> plan= new Stack<>();
		Nodo actual=this.padre;
		while(actual.padre!=null) {
			plan.add(actual.accion_desde_padre);
			actual=actual.padre;
		}
		return plan;
	}
	
}
