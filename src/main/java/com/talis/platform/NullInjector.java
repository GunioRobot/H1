/*
 *    Copyright 2010 Talis Systems Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.talis.platform;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverterBinding;

public class NullInjector implements Injector{
    @Override
    public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> arg0) {
        return null;
    }

    @Override
    public <T> Binding<T> getBinding(Key<T> arg0) {
        return null;
    }

    @Override
    public Map<Key<?>, Binding<?>> getBindings() {
        return null;
    }

    @Override
    public <T> T getInstance(Key<T> arg0) {
        return null;
    }

    @Override
    public <T> T getInstance(Class<T> arg0) {
        return null;
    }

    @Override
    public <T> Provider<T> getProvider(Key<T> arg0) {
        return null;
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> arg0) {
        return null;
    }

    @Override
    public void injectMembers(Object arg0) {
        // Do nothing
    }

	@Override
	public Injector createChildInjector(Iterable<? extends Module> arg0) {
		return null;
	}

	@Override
	public Injector createChildInjector(Module... arg0) {
		return null;
	}

	@Override
	public <T> Binding<T> getBinding(Class<T> arg0) {
		return null;
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> arg0) {
		return null;
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(Class<T> arg0) {
		return null;
	}

	@Override
	public Injector getParent() {
		return null;
	}

	@Override
	public Map<Key<?>, Binding<?>> getAllBindings() {
		return null;
	}

	@Override
	public <T> Binding<T> getExistingBinding(Key<T> key) {
		return null;
	}

	@Override
	public Map<Class<? extends Annotation>, Scope> getScopeBindings() {
		return null;
	}

	@Override
	public Set<TypeConverterBinding> getTypeConverterBindings() {
		return null;
	}
}
