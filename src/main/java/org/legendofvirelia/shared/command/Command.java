package org.legendofvirelia.shared.command;

public interface Command<T> {
    void execute(T world);
    
} 