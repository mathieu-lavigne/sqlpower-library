/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.object.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.helper.SPPersisterHelper;
import ca.sqlpower.object.SPObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.ConstructorDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.EnumConstantDeclaration;
import com.sun.mirror.declaration.EnumDeclaration;
import com.sun.mirror.declaration.ExecutableDeclaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.InterfaceDeclaration;
import com.sun.mirror.declaration.MemberDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.PackageDeclaration;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.ClassType;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.type.PrimitiveType;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.DeclarationVisitor;

/**
 * This {@link DeclarationVisitor} is used to visit {@link SPObject} classes
 * annotated with {@link Persistable}. For each constructor and method, this
 * visitor looks for the {@link Constructor}, {@link ConstructorParameter},
 * {@link Accessor} and {@link Mutator} annotations and keeps track of data
 * required by the {@link SPAnnotationProcessor} to generate
 * {@link SPPersisterHelper} classes.
 */
public class SPClassVisitor implements DeclarationVisitor {
	
	/**
	 * @see #isValid()
	 */
	private boolean valid = true;

	/**
	 * @see #getVisitedClass()
	 */
	private Class<? extends SPObject> visitedClass;
	
	/**
	 * @see #getConstructorParameters()
	 */
	private List<ConstructorParameterObject> constructorParameters = new ArrayList<ConstructorParameterObject>();
	
	/**
	 * @see #getPropertiesToAccess()
	 */
	private Map<String, Class<?>> propertiesToAccess = new HashMap<String, Class<?>>();
	
	/**
	 * @see #getAccessorAdditionalInfo()
	 */
	private Multimap<String, String> accessorAdditionalInfo = LinkedHashMultimap.create();
	
	/**
	 * @see #getPropertiesToMutate()
	 */
	private Map<String, Class<?>> propertiesToMutate = new HashMap<String, Class<?>>();
	
	/**
	 * @see #getMutatorExtraParameters()
	 */
	private Multimap<String, MutatorParameterObject> mutatorExtraParameters = LinkedHashMultimap.create();
	
	/**
	 * @see #getMutatorThrownTypes()
	 */
	private Multimap<String, Class<? extends Exception>> mutatorThrownTypes = HashMultimap.create();
	
	/**
	 * @see #getConstructorImports()
	 */
	private Set<String> constructorImports = new HashSet<String>();
	
	/**
	 * @see #getAccessorImports()
	 */
	private Multimap<String, String> accessorImports = HashMultimap.create();
	
	/**
	 * @see #getMutatorImports()
	 */
	private Multimap<String, String> mutatorImports = HashMultimap.create();
	
	/**
	 * @see #propertiesToPersistOnlyIfNonNull
	 */
	private Set<String> propertiesToPersistOnlyIfNonNull = new HashSet<String>();
	
	/**
	 * Returns whether the visited class along with all its annotated elements is valid.
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Returns the {@link SPObject} class this {@link DeclarationVisitor} is
	 * visiting to parse annotations.
	 */
	public Class<? extends SPObject> getVisitedClass() {
		return visitedClass;
	}
	
	/**
	 * Returns the {@link List} of constructor arguments that are required to
	 * create a new {@link SPObject} of type {@link #visitedClass}. The order of
	 * this list is guaranteed.
	 */
	public List<ConstructorParameterObject> getConstructorParameters() {
		return Collections.unmodifiableList(constructorParameters);
	}

	/**
	 * Returns the {@link Map} of JavaBean getter method names that access
	 * persistable properties, mapped to their property types.
	 */
	public Map<String, Class<?>> getPropertiesToAccess() {
		return Collections.unmodifiableMap(propertiesToAccess);
	}

	/**
	 * Returns the {@link Multimap} of JavaBean getter method names that access
	 * persistable properties, mapped to additional property names that the
	 * session {@link SPPersister} requires to convert the accessor's returned
	 * value into a basic persistable type. The order of this {@link Multimap}
	 * is guaranteed.
	 * 
	 * @see Accessor#additionalInfo()
	 */
	public Multimap<String, String> getAccessorAdditionalInfo() {
		return Multimaps.unmodifiableMultimap(accessorAdditionalInfo);
	}

	/**
	 * Returns the {@link Map} of JavaBean setter method names that mutate
	 * persistable properties, mapped to their property types.
	 */
	public Map<String, Class<?>> getPropertiesToMutate() {
		return Collections.unmodifiableMap(propertiesToMutate);
	}

	/**
	 * Returns the {@link Multimap} of JavaBean setter method names that mutate
	 * persistable properties, mapped to {@link MutatorParameterObject}s that
	 * contain values for an {@link SPPersister} to use. The order of this
	 * {@link Multimap} is guaranteed.
	 */
	public Multimap<String, MutatorParameterObject> getMutatorExtraParameters() {
		return Multimaps.unmodifiableMultimap(mutatorExtraParameters);
	}

	/**
	 * Returns the {@link Multimap} of JavaBean setter method names that mutate
	 * persistable properties, mapped to the method's thrown types.
	 */
	public Multimap<String, Class<? extends Exception>> getMutatorThrownTypes() {
		return Multimaps.unmodifiableMultimap(mutatorThrownTypes);
	}

	/**
	 * Returns the {@link Set} of imports that are required for an
	 * {@link SPPersisterHelper} to use that deals with {@link SPObject}s of
	 * type {@link #visitedClass} which includes dependencies of the
	 * {@link ConstructorParameter} annotated constructor parameters.
	 */
	public Set<String> getConstructorImports() {
		return Collections.unmodifiableSet(constructorImports);
	}

	/**
	 * Returns the {@link Multimap} of {@link Accessor} annotated getter methods to
	 * required imports needed to generate {@link SPPersisterHelper}s that deal
	 * with {@link SPObject}s of type {@link #visitedClass}.
	 */
	public Multimap<String, String> getAccessorImports() {
		return accessorImports;
	}

	/**
	 * Returns the {@link Multimap} of {@link Mutator} annotated setter methods to
	 * required imports needed to generate {@link SPPersisterHelper}s that deal
	 * with {@link SPObject}s of type {@link #visitedClass}, which include
	 * thrown exception types.
	 */
	public Multimap<String, String> getMutatorImports() {
		return mutatorImports;
	}

	/**
	 * Returns the {@link Set} of persistable properties that can only be
	 * persisted if its value is not null.
	 */
	public Set<String> getPropertiesToPersistOnlyIfNonNull() {
		return Collections.unmodifiableSet(propertiesToPersistOnlyIfNonNull);
	}

	/**
	 * Resets all the fields within this class visitor. All the information
	 * about the visited class type, constructor, accessors and mutators will be
	 * wiped. This is to be used when processing classes that contain nested
	 * classes.
	 */
	private void reset() {
		valid = true;
		visitedClass = null;
		propertiesToAccess.clear();
		propertiesToPersistOnlyIfNonNull.clear();
		accessorAdditionalInfo.clear();
		propertiesToMutate.clear();
		mutatorThrownTypes.clear();
		mutatorExtraParameters.clear();
		constructorParameters.clear();
		constructorImports.clear();
		accessorImports.clear();
		mutatorImports.clear();
	}

	/**
	 * Stores the class reference of a {@link Persistable} {@link SPObject} for
	 * use in annotation processing in the {@link SPAnnotationProcessor}. The
	 * processor takes this information to generate {@link SPPersisterHelper}s.
	 * 
	 * @param d
	 *            The {@link ClassDeclaration} of the class to visit.
	 */
	public void visitClassDeclaration(ClassDeclaration d) {
		if (visitedClass != null) {
			reset();
		}
		
		if (d.getAnnotation(Persistable.class) != null) {
			try {
				String qualifiedName = 
					SPAnnotationProcessorUtils.convertTypeDeclarationToQualifiedName(d);
				visitedClass = (Class<? extends SPObject>) Class.forName(qualifiedName);
				
			} catch (ClassNotFoundException e) {
				valid = false;
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stores information about constructors annotated with {@link Constructor},
	 * particularly with the {@link ConstructorParameter} annotated parameters
	 * and their required imports. The {@link SPAnnotationProcessor} takes this
	 * information and generates
	 * {@link SPPersisterHelper#commitObject(ca.sqlpower.dao.PersistedSPObject, Multimap, List, ca.sqlpower.dao.helper.SPPersisterHelperFactory)}
	 * and
	 * {@link SPPersisterHelper#persistObject(SPObject, int, SPPersister, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
	 * methods.
	 * 
	 * @param d
	 *            The {@link ConstructorDeclaration} of the constructor to
	 *            visit.
	 */
	public void visitConstructorDeclaration(ConstructorDeclaration d) {
		
		// If there are nested classes, we need to clear the buffer of
		// constructor parameters as this class visitor visits all classes
		// underneath the top level class.
		if (visitedClass != null) {
			reset();
		}
		
		if (d.getAnnotation(Constructor.class) != null) {
			
			for (ParameterDeclaration pd : d.getParameters()) {
				ConstructorParameter cp = pd.getAnnotation(ConstructorParameter.class);
				if (cp != null) {
					try {
						TypeMirror type = pd.getType();
						Class<?> c = SPAnnotationProcessorUtils.convertTypeMirrorToClass(type);
						String value = null;
						
						boolean property = cp.isProperty();
						String name;
						
						if (property) {
							name = cp.propertyName();
						} else {
							name = pd.getSimpleName();
							value = cp.defaultValue();
						}

						if (type instanceof PrimitiveType) {
							constructorParameters.add(
									new ConstructorParameterObject(property, c, name, value));

						} else if (type instanceof ClassType || type instanceof InterfaceType) {
							constructorParameters.add(
									new ConstructorParameterObject(property, c, name, null));
							constructorImports.add(c.getName());
						}
					} catch (ClassNotFoundException e) {
						valid = false;
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Stores information about getter and setter methods annotated with
	 * {@link Accessor} and {@link Mutator}. This includes thrown exceptions,
	 * setter parameters annotated with {@link MutatorParameter}, and required
	 * imports. The {@link SPAnnotationProcessor} takes this information and
	 * generates
	 * {@link SPPersisterHelper#commitProperty(SPObject, String, Object, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
	 * and
	 * {@link SPPersisterHelper#findProperty(SPObject, String, ca.sqlpower.dao.session.SessionPersisterSuperConverter)}
	 * methods.
	 * 
	 * @param d
	 *            The {@link MethodDeclaration} of the method to visit.
	 */
	public void visitMethodDeclaration(MethodDeclaration d) {
		Accessor accessorAnnotation = d.getAnnotation(Accessor.class);
		Mutator mutatorAnnotation = d.getAnnotation(Mutator.class);
		TypeMirror type = null;
		
		if (visitedClass != null) {
			// Since this class visitor visits method declarations before
			// class declarations, having visitedClass be non-null means
			// that the method being visited actually belongs to a higher
			// level class and not a lower nested class. Thus, we need to
			// clear any buffer of information about accessors and mutators
			// that belonged to nested classes first before populating them
			// again.
			reset();
		}
		
		if (accessorAnnotation != null) {
			type = d.getReturnType();
		} else if (mutatorAnnotation != null) {
			type = d.getParameters().iterator().next().getType();
		} else {
			return;
		}
		
		String methodName = d.getSimpleName();
		Class<?> c = null;
		
		try {
			
			c = SPAnnotationProcessorUtils.convertTypeMirrorToClass(type);

			if (accessorAnnotation != null) {
				propertiesToAccess.put(methodName, c);
				
				if (accessorAnnotation.persistOnlyIfNonNull()) {
					propertiesToPersistOnlyIfNonNull.add(
							SPAnnotationProcessorUtils.convertMethodToProperty(methodName));
				}
				accessorAdditionalInfo.putAll(
						methodName, Arrays.asList(accessorAnnotation.additionalInfo()));
				
				accessorImports.put(methodName, c.getName());
				
			} else {
				for (ReferenceType refType : d.getThrownTypes()) {
					Class<? extends Exception> thrownType = 
						(Class<? extends Exception>) Class.forName(refType.toString());
					mutatorThrownTypes.put(methodName, thrownType);
					mutatorImports.put(methodName, thrownType.getName());
				}

				propertiesToMutate.put(methodName, c);
				mutatorImports.put(methodName, c.getName());
				
				for (ParameterDeclaration pd : d.getParameters()) {
					MutatorParameter mutatorParameterAnnotation = 
						pd.getAnnotation(MutatorParameter.class);
					
					if (mutatorParameterAnnotation != null) {
						Class<?> extraParamType = 
							SPAnnotationProcessorUtils.convertTypeMirrorToClass(pd.getType());
						mutatorExtraParameters.put(methodName, 
								new MutatorParameterObject(
										extraParamType,
										pd.getSimpleName(), 
										mutatorParameterAnnotation.value()));
						mutatorImports.put(methodName, extraParamType.getName());
					}
				}
			}
			
			if (c.getName().endsWith("SQLObject")) {
				System.out.println("Method importing SQLObject: " + methodName);
			}
			
		} catch (ClassNotFoundException e) {
			valid = false;
			e.printStackTrace();
		}
	}

	public void visitAnnotationTypeDeclaration(AnnotationTypeDeclaration d) {
		// no-op
	}

	public void visitAnnotationTypeElementDeclaration(AnnotationTypeElementDeclaration d) {
		// no-op
	}

	public void visitDeclaration(Declaration d) {
		// no-op
	}

	public void visitEnumConstantDeclaration(EnumConstantDeclaration d) {
		// no-op
	}

	public void visitEnumDeclaration(EnumDeclaration d) {
		// no-op
	}

	public void visitExecutableDeclaration(ExecutableDeclaration d) {
		// no-op
	}

	public void visitFieldDeclaration(FieldDeclaration d) {
		// no-op		
	}

	public void visitInterfaceDeclaration(InterfaceDeclaration d) {
		// no-op		
	}

	public void visitMemberDeclaration(MemberDeclaration d) {
		// no-op		
	}

	public void visitPackageDeclaration(PackageDeclaration d) {
		// no-op		
	}

	public void visitParameterDeclaration(ParameterDeclaration d) {
		// no-op		
	}

	public void visitTypeDeclaration(TypeDeclaration d) {
		// no-op		
	}

	public void visitTypeParameterDeclaration(TypeParameterDeclaration d) {
		// no-op		
	}

}
