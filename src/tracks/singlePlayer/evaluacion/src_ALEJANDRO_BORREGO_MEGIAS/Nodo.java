package tracks.singlePlayer.evaluacion.src_ALEJANDRO_BORREGO_MEGIAS;

import java.util.Stack;

import ontology.Types;
import tools.Vector2d;

public class Nodo {
	public Vector2d coordenadas;
	public Boolean estado;
	public Types.ACTIONS accion_desde_padre;
	public Nodo padre;
	
	//TODO Explorar cómo se hacen más constructores
	public Nodo(Vector2d coord, Boolean est, Types.ACTIONS accion, Nodo padre) {
		this.coordenadas=coord;
		this.estado=est;
		this.accion_desde_padre=accion;
		this.padre=padre;
	}
	
	public Boolean equals(Nodo n) {
		if(this.coordenadas.x==n.coordenadas.x && this.coordenadas.y==n.coordenadas.y)
			return true;
		else
			return false;
	}
	
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
