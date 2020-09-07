package vg.civcraft.mc.civmodcore.locations.spatial.quadtrees;

import vg.civcraft.mc.civmodcore.locations.spatial.IIntBBox2D;
import vg.civcraft.mc.civmodcore.locations.spatial.IIntBBox3D;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author psygate
 */
public final class QuadTree<ValueType extends IIntBBox2D> implements Collection<ValueType> {
	private IIntBBox2D area;
	private AreaQuadTreeNode<ValueType> root;
	private final int splitSize;
	private int size = 0;

	public QuadTree(IIntBBox2D area, int splitSize) {
		assert splitSize > 1;
		this.area = area;
		root = new AreaQuadTreeNode<>(area, splitSize);
		this.splitSize = splitSize;
	}

	public void addThrowing(ValueType value) {
		if (!add(value)) {
			throw new IllegalArgumentException("Object " + value + " not contained in OcTree area.");
		}
	}

	@Override
	public boolean add(ValueType value) {
		Objects.requireNonNull(value);

		if (!IIntBBox2D.contains(root, value)) {
			return false;
		} else {
			AreaQuadTreeNode<ValueType> insertionNode = selectNodeContainingBBox(value);
			insertionNode.addValue(value);
			insertionNode.splitIfNecessary();
			size++;
			return true;
		}
	}

	private AreaQuadTreeNode<ValueType> selectNodeContainingBBox(ValueType value) {
		assert IIntBBox2D.contains(area, value);

		PredicateNodeIterator<AreaQuadTreeNode<ValueType>, ValueType> it = new PredicateNodeIterator<>(root, node -> IIntBBox2D.contains(node, value));
		AreaQuadTreeNode<ValueType> selected = root;

		while (it.hasNext()) {
			AreaQuadTreeNode<ValueType> next = it.next();

			if (IIntBBox2D.contains(next, value)) {
				selected = next;
			}
		}

		return selected;
	}

	public int size() {
		return size;
	}

	public void clear() {
		root = new AreaQuadTreeNode<>(area, splitSize);
		size = 0;
	}

	@Override
	public Spliterator<ValueType> spliterator() {
		return null;
	}

	@Override
	public Stream<ValueType> stream() {
		return null;
	}

	@Override
	public Stream<ValueType> parallelStream() {
		return null;
	}

	//test method to assert that the counted size is the real size. very costly to run.
	int countSize() {
		int size = 0;
		NodeIterator<AreaQuadTreeNode<ValueType>, ValueType> nodeIt = new NodeIterator<>(root);
		while (nodeIt.hasNext()) {
			size += nodeIt.next().values().size();
		}

		return size;
	}

	int countNodes() {
		int size = 0;
		LinkedList<AreaQuadTreeNode<ValueType>> stack = new LinkedList<>();
		stack.add(root);

		while (!stack.isEmpty()) {
			AreaQuadTreeNode<ValueType> node = stack.pop();
			if (node.hasChildren()) {
				stack.addAll(node.getChildren());
			}
			size++;
		}

		return size;
	}

	AreaQuadTreeNode<ValueType> getRoot() {
		return root;
	}

	int getSplitSize() {
		return splitSize;
	}

	/**
	 * Selects all values contained within the provided volume box.
	 *
	 * @param box      Volume in which the values should be contained.
	 * @param parallel If the stream evaluation should happen in parallel.
	 * @return A stream containing all values in the tree, that are contained within the provided volume.
	 */
	public Stream<ValueType> selectAllInVolume(IIntBBox2D box, boolean parallel) {
		return selectByPredicate(box::contains, box::intersects, parallel);
	}

	/**
	 * Selects all values intersecting with the provided volume box.
	 *
	 * @param box      Volume with which the values should intersect.
	 * @param parallel If the stream evaluation should happen in parallel.
	 * @return A stream containing all values in the tree, that are intersecting with the provided volume.
	 */
	public Stream<ValueType> selectAllIntersectingVolume(IIntBBox2D box, boolean parallel) {
		return selectByPredicate(box::intersects, box::intersects, parallel);
	}

	/**
	 * Selects all values that contain the given point.
	 *
	 * @param x        X-Coordinate of the point.
	 * @param y        Y-Coordinate of the point.
	 * @param parallel If the stream evaluation should happen in parallel.
	 * @return A stream containing all values in the tree, that contain the given point.
	 */
	public Stream<ValueType> selectAllContainingPoint(int x, int y, boolean parallel) {
		return selectByPredicate(box -> box.contains(x, y), box -> box.contains(x, y), parallel);
	}

	/**
	 * Selects values by predicate and returns a stream of those values.
	 *
	 * @param nodeSelectionPredicate  Predicate that evaluates on the bounding box of tree nodes. A node is only iterated
	 *                                over if this predicate evaluates to true.
	 * @param valueSelectionPredicate Predicate that evaluates on the values in a tree node selected by the nodeSelectionPredicate,
	 *                                only values for which this predicate is true are returned in the stream.
	 * @param parallel                If the stream evaluation should happen in parallel.
	 * @return A stream containing all values for which the valueSelectionPredicate is true, and which are in a tree node,
	 * for which the nodeSelectionPredicate is true.
	 */
	public Stream<ValueType> selectByPredicate(Predicate<IIntBBox2D> nodeSelectionPredicate, Predicate<ValueType> valueSelectionPredicate, boolean parallel) {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
						new PredicateValueIterator<>(
								root,
								valueSelectionPredicate,
								nodeSelectionPredicate),
						Spliterator.IMMUTABLE | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED
				)
				, parallel);
	}

	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof IIntBBox2D) {
			IIntBBox2D box = (IIntBBox2D) o;
			PredicateValueIterator<AreaQuadTreeNode<ValueType>, ValueType> it = new PredicateValueIterator<>(
					root,
					value -> value.equals(box),
					node -> node.contains(box)
			);

			return it.hasNext();
		} else {
			return false;
		}
	}

	@Override
	public Iterator<ValueType> iterator() {
		return new ValueIterator<>(root);
	}

	@Override
	public void forEach(Consumer<? super ValueType> action) {
		stream().forEach(action);
	}

	@Override
	public Object[] toArray() {
		Object[] out = new Object[size()];
		Iterator<ValueType> it = iterator();

		for (int i = 0; i < size(); i++) {
			out[i] = it.next();
		}

		return out;
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		final T1[] arr;
		if (a.length < size()) {
			arr = Arrays.copyOf(a, size());
		} else {
			arr = a;
		}

		Iterator<ValueType> it = iterator();

		for (int i = 0; i < size(); i++) {
			arr[i] = (T1) it.next();
		}

		return arr;
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof IIntBBox2D) {
			IIntBBox2D box = (IIntBBox2D) o;
			if (removeInternal(box)) {
				rebuildTree();
				size--;
				return true;
			}
		}

		return false;
	}

	private boolean removeInternal(IIntBBox2D box) {
		PredicateNodeIterator<AreaQuadTreeNode<ValueType>, ValueType> nodeIterator = new PredicateNodeIterator<>(root, node -> node.contains(box));

		while (nodeIterator.hasNext()) {
			AreaQuadTreeNode<ValueType> node = nodeIterator.next();
			if (node.remove(box)) {
				return true;
			}
		}

		return false;
	}

	private void rebuildTree() {
		root.rebuildRecursively();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return c.stream().allMatch(this::contains);
	}

	@Override
	public boolean addAll(Collection<? extends ValueType> c) {
		return c.stream().allMatch(this::add);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return removeIf(c::contains);
	}

	@Override
	public boolean removeIf(Predicate<? super ValueType> filter) {
		boolean mod = false;
		ValueIterator<AreaQuadTreeNode<ValueType>, ValueType> it = new ValueIterator<>(root);
		while (it.hasNext()) {
			if (filter.test(it.next())) {
				it.remove();
				mod = true;
			}
		}

		if (mod) rebuildTree();

		return mod;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return removeIf(value -> !c.contains(value));
	}
}
