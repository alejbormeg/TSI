package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import java.util.Stack;

import ontology.Types;
import tools.Vector2d;

public class Nodo implements Comparable<Nodo>{
	public Vector2d coordenadas; //Coordenadas 2d del nodo
	public Types.ACTIONS accion_desde_padre; //Acción realizada desde el padre para llegar al nodo
	public Nodo padre; //puntero al nodo padre
	public double id; //Identificador único
	public double f;
	public double g;
	public double h;
	
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
		this.f=0;
		this.g=0;
		this.h=0;
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
		this.f=0;
		this.g=0;
		this.h=0;
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
		this.f=n.f;
		this.g=n.g;
		this.h=n.h;
	}
	
	//Constructores para A*
	/**
	 * Constructor con parámetros
	 * @param n nodo del que vamos a hacer la copia
	 */
	public Nodo(Vector2d coordenadas,double g,double h) {
		this.coordenadas=coordenadas;
		this.accion_desde_padre=Types.ACTIONS.ACTION_NIL;
		this.padre=null;
		this.id=10000*this.coordenadas.x+coordenadas.y;
		this.g=g;
		this.h=h;
		this.f=g+h;
	}
	
	/**
	 * Constructor con parámetros
	 * @param coord coordenadas 2d
	 * @param accion Acción realizada desde el padre
	 * @param padre Nodo padre
	 */
	public Nodo(Vector2d coord, Types.ACTIONS accion, Nodo padre, double g, double h) {
		this.coordenadas=coord;
		this.accion_desde_padre=accion;
		this.padre=padre;
		this.id=10000*this.coordenadas.x+coordenadas.y;
		this.f=g+h;
		this.g=g;
		this.h=h;
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
	
	
	/**
	 * Compara un nodo con otro con objeto de ordenarlos
	 * @param n Nodo con el que comparar
	 * @return devuelve 0 si son iguales, -1 si this es mayor y 1 en caso contrario
	 */
	@Override
	public int compareTo(Nodo n) {
			
		if(this.f>=n.f) {
			if(this.f>n.f)
				return -1;
			else if(this.g>n.g) {
				return -1;
			} else if(this.g==n.g){
				return 1; //Si pongo 0 no mete los elementos en el set
			}else {
				return 1;
			}
		}
		else
			return 1;
	
	}
	
	
}
